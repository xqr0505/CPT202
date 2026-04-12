package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.constant.CommonConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author QiranXiao
 * @since 2026/4/1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final BookingMapper bookingMapper;
    private final BookingTopicMapper bookingTopicMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final SpecialistQueryService specialistQueryService;

    @Override
    public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<UpcomingBookingVO> result = bookingMapper.selectUpcomingBookings(customerId, BookingStatusEnum.CONFIRMED.name(), now, limit);

        if (result != null) {
            result.forEach(booking -> booking.setToday(booking.getStartTime().toLocalDate().isEqual(now.toLocalDate())));
        }

        return result != null ? result : List.of();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingCreateVO createBooking(Long customerId, BookingCreateDTO createDTO) {
        TimeSlot slot = timeSlotMapper.selectById(createDTO.getSlotId());
        if (slot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Time slot not found");
        }
        if (!createDTO.getSpecialistId().equals(slot.getSpecialistId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Slot does not belong to the selected specialist");
        }
        if (!TimeSlotStatusEnum.AVAILABLE.name().equals(slot.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Time slot already booked");
        }
        String normalizedTopic = normalizeTopic(createDTO.getTopic());
        validateTopic(normalizedTopic);
        String normalizedNotes = normalizeNotes(createDTO.getCustomerNotes());

        SpecialistDetailVO specialist = specialistQueryService.getSpecialistDetail(createDTO.getSpecialistId());

        Booking booking = Booking.builder()
                .customerId(customerId)
                .specialistId(createDTO.getSpecialistId())
                .slotId(createDTO.getSlotId())
                .status(BookingStatusEnum.PENDING.name())
                .price(resolvePrice(specialist.getConsultationFee()))
                .topic(normalizedTopic)
                .customerNotes(normalizedNotes)
                .parentBookingId(null)
                .decisionTime(null)
                .cancelledBy(null)
                .cancelReason(null)
                .changeType(null)
                .refundStatus("NONE")
                .rejectionReason(null)
                .build();
        bookingMapper.insert(booking);

        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        int updated = timeSlotMapper.update(
                slot,
                Wrappers.<TimeSlot>lambdaUpdate()
                        .eq(TimeSlot::getId, slot.getId())
                        .eq(TimeSlot::getStatus, TimeSlotStatusEnum.AVAILABLE.name())
        );
        if (updated == 0) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Time slot already booked");
        }

        return new BookingCreateVO(booking.getId(), booking.getStatus());
    }

    @Override
    public PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto) {
        LocalDateTime currentTime = LocalDateTime.now();
        long offset = (long) (dto.getPageNo() - 1) * dto.getPageSize();
        List<BookingItemVO> list = bookingMapper.selectBookingList(customerId, dto.getTab(), dto.getStatus(), currentTime, offset, dto.getPageSize());
        Long total = bookingMapper.selectBookingListCount(customerId, dto.getTab(), dto.getStatus(), currentTime);
        return new PageResult<>(total, list);
    }

    @Override
    public UsageSummaryVO getUsageSummary(UsageSummaryQueryDTO queryDTO) {
        validateUsageSummaryDateRange(queryDTO);

        Long customerId = SecurityUtils.getCurrentUserId();
        String completedStatus = BookingStatusEnum.COMPLETED.name();
        LocalDate startDate = queryDTO == null ? null : queryDTO.getStartDate();
        LocalDate endDate = queryDTO == null ? null : queryDTO.getEndDate();

        UsageSummaryVO summary = Optional.ofNullable(
                        bookingMapper.selectUsageSummary(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(UsageSummaryVO::new);

        List<UsageSummaryVO.ConsultedExpertVO> consultedExperts = Optional.ofNullable(
                        bookingMapper.selectConsultedExperts(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(ArrayList::new);

        summary.setTotalCompletedAppointments(Optional.ofNullable(summary.getTotalCompletedAppointments()).orElse(CommonConstant.NO));
        summary.setTotalAmountSpent(Optional.ofNullable(summary.getTotalAmountSpent()).orElse(BigDecimal.ZERO));
        summary.setTotalConsultationHours(Optional.ofNullable(summary.getTotalConsultationHours()).orElse((double) CommonConstant.NO));
        summary.setConsultedExperts(consultedExperts);
        return summary;
    }

    public BookingDetailVO getBookingDetailById(Long bookingId, Long currentCustomerId) {
        BookingDetailVO detail = bookingMapper.selectBookingDetailById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found: bookingId={}", bookingId);
                    return new BusinessException(ResultCodeEnum.NOT_FOUND);
                });

        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !booking.getCustomerId().equals(currentCustomerId)) {
            log.warn("Data isolation violation detected: customerId={}, bookingId={}, actual customerId={}",
                    currentCustomerId, bookingId, booking != null ? booking.getCustomerId() : "N/A");
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }

        return detail;
    }

    private BigDecimal resolvePrice(BigDecimal consultationFee) {
        return consultationFee == null ? BigDecimal.ZERO : consultationFee;
    }

    private String normalizeTopic(String topic) {
        return topic == null ? "" : topic.trim();
    }

    private String normalizeNotes(String customerNotes) {
        if (customerNotes == null) {
            return null;
        }
        String normalized = customerNotes.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateTopic(String topic) {
        Long allowedTopicCount = bookingTopicMapper.countActiveTopicByName(topic);
        if (allowedTopicCount == null || allowedTopicCount == 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Topic is not available for this specialist");
        }
    }

    private void validateUsageSummaryDateRange(UsageSummaryQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getStartDate() == null || queryDTO.getEndDate() == null) {
            return;
        }
        if (queryDTO.getStartDate().isAfter(queryDTO.getEndDate())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}

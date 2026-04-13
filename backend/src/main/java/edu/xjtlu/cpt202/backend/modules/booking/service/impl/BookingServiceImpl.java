package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.booking.service.CustomerBookingChangePolicyService;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
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
import java.time.LocalTime;
import java.util.List;

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
    private final CustomerBookingChangePolicyService customerBookingChangePolicyService;

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

    @Override
    public BookingCancelQuoteVO customerCancellationQuote(Long bookingId, Long currentCustomerId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("Booking not found for cancel quote: bookingId={}", bookingId);
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (!booking.getCustomerId().equals(currentCustomerId)) {
            log.warn("Cancel quote forbidden: customerId={}, bookingId={}", currentCustomerId, bookingId);
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
        TimeSlot slot = timeSlotMapper.selectById(booking.getSlotId());
        if (slot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Time slot not found");
        }
        LocalDateTime slotStart = resolveSlotStart(slot);
        return customerBookingChangePolicyService.customerCancellationQuote(
                booking.getStatus(),
                slotStart,
                LocalDateTime.now(),
                booking.getPrice()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingCancelConfirmVO customerCancellationConfirm(Long bookingId, Long currentCustomerId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("Booking not found for cancel confirm: bookingId={}", bookingId);
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }

        TimeSlot slot = timeSlotMapper.selectById(booking.getSlotId());
        if (slot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Time slot not found");
        }

        if (!booking.getCustomerId().equals(currentCustomerId)) {
            log.warn("Cancel confirm forbidden: customerId={}, bookingId={}", currentCustomerId, bookingId);
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        TimeSlot slotToUpdate = new TimeSlot();
        
        //customer cancellation quote
        BookingCancelQuoteVO quote = customerBookingChangePolicyService.customerCancellationQuote(
                booking.getStatus(),
                resolveSlotStart(slot),
                now,
                booking.getPrice()
        );
        
        if (!quote.isAllowed()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), quote.getMessage());
        }

        booking.setStatus(BookingStatusEnum.CANCELLED.name());
        booking.setCancelledBy("CUSTOMER");
        booking.setChangeType("CANCEL");
        booking.setDecisionTime(now);
        bookingMapper.updateById(booking);

        slotToUpdate.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        int updated = timeSlotMapper.update(
                slotToUpdate,
                Wrappers.<TimeSlot>lambdaUpdate()
                        .eq(TimeSlot::getId, slot.getId())
                        .eq(TimeSlot::getStatus, TimeSlotStatusEnum.BOOKED.name())
        );

        if (updated == 0) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Failed to release booked slot");
        }

        return BookingCancelConfirmVO.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatusEnum.CANCELLED.name())
                .policyType(quote.getPolicyType())
                .refundAmount(quote.getRefundAmount())
                .penaltyAmount(quote.getPenaltyAmount())
                .message(quote.getMessage())
                .build();
    }

    private static LocalDateTime resolveSlotStart(TimeSlot slot) {
        LocalDate date = slot.getSlotDate();
        LocalTime start = slot.getStartTime();
        if (date == null || start == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Time slot has no start time");
        }
        return LocalDateTime.of(date, start);
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
}

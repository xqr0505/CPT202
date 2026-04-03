package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author QiranXiao
 * @since 2026/4/1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private static final Map<Long, List<String>> SPECIALIST_TOPICS = Map.of(
            1L, Arrays.asList("Career Planning", "Study Abroad"),
            201L, Arrays.asList("Career Planning", "Study Abroad"),
            202L, Arrays.asList("Mental Wellness", "Stress Management"),
            203L, Arrays.asList("Study Abroad", "Career Planning")
    );

    private final BookingMapper bookingMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final SpecialistQueryService specialistQueryService;

    @Override
    public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<UpcomingBookingVO> result = bookingMapper.selectUpcomingBookings(customerId, BookingStatusEnum.CONFIRMED.name(), now, limit);

        if (result != null) {
            result.forEach(booking -> {
                booking.setToday(booking.getStartTime().toLocalDate().isEqual(now.toLocalDate()));
            });
        }

        return result != null ? result : List.of();
    }

    @Override
    public PageResult<BookingHistoryListVO> listBookings(Long customerId, BookingHistoryQueryDTO queryDTO) {
        Page<BookingHistoryListVO> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        IPage<BookingHistoryListVO> resultPage = bookingMapper.listBookings(page, queryDTO, customerId);
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public BookingDetailVO getBookingDetail(Long bookingId, Long customerId) {
        BookingDetailVO detail = bookingMapper.getBookingDetail(bookingId, customerId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
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
        validateTopic(createDTO.getSpecialistId(), createDTO.getTopic());

        SpecialistDetailVO specialist = specialistQueryService.getSpecialistDetail(createDTO.getSpecialistId());

        Booking booking = Booking.builder()
                .customerId(customerId)
                .specialistId(createDTO.getSpecialistId())
                .slotId(createDTO.getSlotId())
                .status(BookingStatusEnum.PENDING.name())
                .price(resolvePrice(specialist.getConsultationFee()))
                .topic(createDTO.getTopic().trim())
                .customerNotes(createDTO.getCustomerNotes())
                .parentBookingId(null)
                .decisionTime(LocalDateTime.now())
                .cancelledBy("")
                .cancelReason("")
                .changeType("")
                .refundStatus("NONE")
                .rejectionReason("")
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

    private BigDecimal resolvePrice(BigDecimal consultationFee) {
        return consultationFee == null ? BigDecimal.ZERO : consultationFee;
    }

    private void validateTopic(Long specialistId, String topic) {
        List<String> allowedTopics = SPECIALIST_TOPICS.get(specialistId);
        if (allowedTopics == null || allowedTopics.isEmpty()) {
            return;
        }
        if (topic == null || !allowedTopics.contains(topic.trim())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Topic is not available for this specialist");
        }
    }
}

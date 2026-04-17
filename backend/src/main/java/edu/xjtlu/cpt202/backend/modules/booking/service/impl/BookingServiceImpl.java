package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.constant.CommonConstant;
import edu.xjtlu.cpt202.backend.common.constant.RedisCacheConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.utils.RedisKeyUtils;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.constant.DashboardConstant;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardHabitRawVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.booking.service.CustomerBookingChangePolicyService;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author QiranXiao
 * @since 2026/4/1
 */
@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingMapper bookingMapper;
    private final BookingTopicMapper bookingTopicMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final SpecialistQueryService specialistQueryService;
    private final CustomerBookingChangePolicyService customerBookingChangePolicyService;
    private final RedisTemplate<String, Object> jsonRedisTemplate;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            BookingTopicMapper bookingTopicMapper,
            TimeSlotMapper timeSlotMapper,
            SpecialistQueryService specialistQueryService,
            CustomerBookingChangePolicyService customerBookingChangePolicyService,
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> jsonRedisTemplate
    ) {
        this.bookingMapper = bookingMapper;
        this.bookingTopicMapper = bookingTopicMapper;
        this.timeSlotMapper = timeSlotMapper;
        this.specialistQueryService = specialistQueryService;
        this.customerBookingChangePolicyService = customerBookingChangePolicyService;
        this.jsonRedisTemplate = jsonRedisTemplate;
    }

    @Override
    public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<UpcomingBookingVO> result = bookingMapper.selectUpcomingBookings(
                customerId,
                BookingStatusEnum.CONFIRMED.name(),
                now,
                limit
        );

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

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setSpecialistId(createDTO.getSpecialistId());
        booking.setSlotId(createDTO.getSlotId());
        booking.setStatus(BookingStatusEnum.PENDING.name());
        booking.setPrice(resolvePrice(specialist.getConsultationFee()));
        booking.setTopic(normalizedTopic);
        booking.setCustomerNotes(normalizedNotes);
        booking.setParentBookingId(null);
        booking.setDecisionTime(null);
        booking.setCancelledBy(null);
        booking.setCancelReason(null);
        booking.setChangeType(null);
        booking.setRefundStatus("NONE");
        booking.setRejectionReason(null);
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

        invalidateCustomerBookingCache(customerId);
        return new BookingCreateVO(booking.getId(), booking.getStatus());
    }

    @Override
    public PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto) {
        String cacheKey = RedisKeyUtils.buildCustomerBookingListKey(
                customerId,
                dto.getPageNo(),
                dto.getPageSize(),
                dto.getTab(),
                dto.getStatus()
        );

        Optional<PageResult> cachedPageResult = readCache(cacheKey, PageResult.class);
        if (cachedPageResult.isPresent()) {
            //noinspection unchecked
            return (PageResult<BookingItemVO>) cachedPageResult.get();
        }

        LocalDateTime currentTime = LocalDateTime.now();
        long offset = (long) (dto.getPageNo() - 1) * dto.getPageSize();
        List<BookingItemVO> list = bookingMapper.selectBookingList(
                customerId,
                dto.getTab(),
                dto.getStatus(),
                currentTime,
                offset,
                dto.getPageSize()
        );
        Long total = bookingMapper.selectBookingListCount(customerId, dto.getTab(), dto.getStatus(), currentTime);
        PageResult<BookingItemVO> pageResult = new PageResult<>(total, list);
        writeCache(cacheKey, pageResult, RedisCacheConstant.BOOKING_LIST_CACHE_TTL_SECONDS);
        return pageResult;
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

    @Override
    public DashboardStatisticsVO getDashboardStatistics(DashboardQueryDTO queryDTO) {
        validateDashboardDateRange(queryDTO);

        Long customerId = SecurityUtils.getCurrentUserId();
        String completedStatus = BookingStatusEnum.COMPLETED.name();
        LocalDate startDate = queryDTO == null ? null : queryDTO.getStartDate();
        LocalDate endDate = queryDTO == null ? null : queryDTO.getEndDate();

        DashboardStatisticsVO statistics = Optional.ofNullable(
                        bookingMapper.selectDashboardSummary(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(DashboardStatisticsVO::new);

        List<DashboardStatisticsVO.ConsultedExpertVO> consultedExperts = Optional.ofNullable(
                        bookingMapper.selectDashboardConsultedExperts(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(ArrayList::new);

        List<DashboardStatisticsVO.TrendChartVO> trendData = loadTrendData(customerId, completedStatus, startDate, endDate);
        List<DashboardStatisticsVO.CategoryChartVO> categoryData = Optional.ofNullable(
                        bookingMapper.selectDashboardCategoryData(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(ArrayList::new);
        List<DashboardStatisticsVO.HabitChartVO> habitData = buildHabitData(customerId, completedStatus, startDate, endDate);

        statistics.setTotalCompletedAppointments(Optional.ofNullable(statistics.getTotalCompletedAppointments()).orElse(CommonConstant.NO));
        statistics.setTotalAmountSpent(Optional.ofNullable(statistics.getTotalAmountSpent()).orElse(BigDecimal.ZERO));
        statistics.setTotalConsultationHours(Optional.ofNullable(statistics.getTotalConsultationHours()).orElse((double) CommonConstant.NO));
        statistics.setConsultedExperts(consultedExperts);
        statistics.setTrendData(trendData);
        statistics.setCategoryData(categoryData);
        statistics.setHabitData(habitData);
        return statistics;
    }

    @Override
    public BookingDetailVO getBookingDetailById(Long bookingId, Long currentCustomerId) {
        String cacheKey = RedisKeyUtils.buildCustomerBookingDetailKey(currentCustomerId, bookingId);
        Optional<BookingDetailVO> cachedBookingDetail = readCache(cacheKey, BookingDetailVO.class);
        if (cachedBookingDetail.isPresent()) {
            return cachedBookingDetail.get();
        }

        BookingDetailVO detail = bookingMapper.selectBookingDetailById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found: bookingId={}", bookingId);
                    return new BusinessException(ResultCodeEnum.NOT_FOUND);
                });

        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !booking.getCustomerId().equals(currentCustomerId)) {
            log.warn(
                    "Data isolation violation detected: customerId={}, bookingId={}, actual customerId={}",
                    currentCustomerId,
                    bookingId,
                    booking != null ? booking.getCustomerId() : "N/A"
            );
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }

        writeCache(cacheKey, detail, RedisCacheConstant.BOOKING_DETAIL_CACHE_TTL_SECONDS);
        return detail;
    }

    @Override
    public List<SpecialistPendingBookingVO> listPendingRequestsForSpecialist(Long currentUserId) {
        return bookingMapper.selectPendingRequestsForSpecialist(currentUserId, BookingStatusEnum.PENDING.name());
    }

    @Override
    public List<SpecialistHandledBookingVO> listHandledRequestsForSpecialist(Long currentUserId) {
        return bookingMapper.selectHandledRequestsForSpecialist(currentUserId);
    }

    @Override
    public SpecialistBookingDetailVO getBookingRequestDetailForSpecialist(Long bookingId, Long currentUserId) {
        validateBookingOwnershipForSpecialist(bookingId, currentUserId);
        SpecialistBookingDetailVO detail = bookingMapper.selectBookingRequestDetailForSpecialist(bookingId, currentUserId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBookingRequest(Long bookingId, Long currentUserId) {
        Booking booking = loadPendingBookingForSpecialist(bookingId, currentUserId);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setDecisionTime(LocalDateTime.now());
        booking.setRejectionReason(null);
        bookingMapper.updateById(booking);

        TimeSlot timeSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (timeSlot != null) {
            timeSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
            timeSlotMapper.updateById(timeSlot);
        }

        invalidateCustomerBookingCache(booking.getCustomerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBookingRequest(Long bookingId, Long currentUserId, SpecialistRejectBookingRequestDTO requestDTO) {
        Booking booking = loadPendingBookingForSpecialist(bookingId, currentUserId);
        String rejectionReason = requestDTO.getRejectionReason().trim();

        booking.setStatus(BookingStatusEnum.CANCELLED.name());
        booking.setDecisionTime(LocalDateTime.now());
        booking.setRejectionReason(rejectionReason);
        booking.setCancelledBy("SPECIALIST");
        booking.setCancelReason(rejectionReason);
        booking.setChangeType("REJECT");
        bookingMapper.updateById(booking);

        TimeSlot timeSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (timeSlot != null) {
            timeSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
            timeSlotMapper.updateById(timeSlot);
        }

        invalidateCustomerBookingCache(booking.getCustomerId());
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
    public BookingRescheduleQuoteVO customerRescheduleQuote(Long bookingId, Long newSlotId, Long currentCustomerId) {
        if (newSlotId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "newSlotId is required");
        }

        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("Booking not found for reschedule quote: bookingId={}", bookingId);
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (!booking.getCustomerId().equals(currentCustomerId)) {
            log.warn("Reschedule quote forbidden: customerId={}, bookingId={}", currentCustomerId, bookingId);
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }

        TimeSlot currentSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (currentSlot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Time slot not found");
        }
        if (newSlotId.equals(booking.getSlotId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Choose a different time slot to reschedule");
        }

        TimeSlot newSlot = timeSlotMapper.selectById(newSlotId);
        if (newSlot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "New time slot not found");
        }
        if (!booking.getSpecialistId().equals(newSlot.getSpecialistId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Slot does not belong to this booking's specialist");
        }
        if (!TimeSlotStatusEnum.AVAILABLE.name().equals(newSlot.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Time slot is not available");
        }

        SpecialistDetailVO specialist = specialistQueryService.getSpecialistDetail(booking.getSpecialistId());
        BigDecimal newPrice = resolvePrice(specialist.getConsultationFee());

        LocalDateTime slotStart = resolveSlotStart(currentSlot);
        return customerBookingChangePolicyService.customerRescheduleQuote(
                booking.getStatus(),
                slotStart,
                LocalDateTime.now(),
                booking.getPrice(),
                newPrice
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingCancelConfirmVO customerCancellationConfirm(Long bookingId, Long currentCustomerId) {
        Booking booking = bookingMapper.selectById(bookingId);
        LocalDateTime now = LocalDateTime.now();
        TimeSlot slotToUpdate = new TimeSlot();

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

        invalidateCustomerBookingCache(currentCustomerId);
        return BookingCancelConfirmVO.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatusEnum.CANCELLED.name())
                .policyType(quote.getPolicyType())
                .refundAmount(quote.getRefundAmount())
                .penaltyAmount(quote.getPenaltyAmount())
                .message(quote.getMessage())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingRescheduleConfirmVO customerRescheduleConfirm(Long bookingId, Long newSlotId, Long currentCustomerId) {
        if (newSlotId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "newSlotId is required");
        }

        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            log.warn("Booking not found for reschedule confirm: bookingId={}", bookingId);
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (!booking.getCustomerId().equals(currentCustomerId)) {
            log.warn("Reschedule confirm forbidden: customerId={}, bookingId={}", currentCustomerId, bookingId);
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
        if (newSlotId.equals(booking.getSlotId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Choose a different time slot to reschedule");
        }

        TimeSlot currentSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (currentSlot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Time slot not found");
        }
        TimeSlot newSlot = timeSlotMapper.selectById(newSlotId);
        if (newSlot == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "New time slot not found");
        }
        if (!booking.getSpecialistId().equals(newSlot.getSpecialistId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Slot does not belong to this booking's specialist");
        }
        if (!TimeSlotStatusEnum.AVAILABLE.name().equals(newSlot.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Time slot is not available");
        }

        SpecialistDetailVO specialist = specialistQueryService.getSpecialistDetail(booking.getSpecialistId());
        BigDecimal newPrice = resolvePrice(specialist.getConsultationFee());
        LocalDateTime now = LocalDateTime.now();
        BookingRescheduleQuoteVO quote = customerBookingChangePolicyService.customerRescheduleQuote(
                booking.getStatus(),
                resolveSlotStart(currentSlot),
                now,
                booking.getPrice(),
                newPrice
        );
        if (!quote.isAllowed()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), quote.getMessage());
        }

        TimeSlot releaseCurrentSlot = new TimeSlot();
        releaseCurrentSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        int oldSlotUpdated = timeSlotMapper.update(
                releaseCurrentSlot,
                Wrappers.<TimeSlot>lambdaUpdate()
                        .eq(TimeSlot::getId, currentSlot.getId())
                        .eq(TimeSlot::getStatus, TimeSlotStatusEnum.BOOKED.name())
        );
        if (oldSlotUpdated == 0) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Failed to release booked slot");
        }

        TimeSlot occupyNewSlot = new TimeSlot();
        occupyNewSlot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        int newSlotUpdated = timeSlotMapper.update(
                occupyNewSlot,
                Wrappers.<TimeSlot>lambdaUpdate()
                        .eq(TimeSlot::getId, newSlotId)
                        .eq(TimeSlot::getStatus, TimeSlotStatusEnum.AVAILABLE.name())
        );
        if (newSlotUpdated == 0) {
            throw new BusinessException(ResultCodeEnum.BOOKING_ERROR_BLOCK.getCode(), "Time slot is not available");
        }

        booking.setSlotId(newSlotId);
        booking.setStatus(BookingStatusEnum.PENDING.name());
        booking.setChangeType("RESCHEDULE");
        booking.setDecisionTime(now);
        bookingMapper.updateById(booking);

        invalidateCustomerBookingCache(currentCustomerId);
        return BookingRescheduleConfirmVO.builder()
                .bookingId(bookingId)
                .bookingStatus(booking.getStatus())
                .policyType(quote.getPolicyType())
                .priceDifference(quote.getPriceDifference())
                .penaltyAmount(quote.getPenaltyAmount())
                .refundAmount(quote.getRefundAmount())
                .payableAmount(quote.getPayableAmount())
                .message(quote.getMessage())
                .build();
    }

    private Booking loadPendingBookingForSpecialist(Long bookingId, Long currentUserId) {
        validateBookingOwnershipForSpecialist(bookingId, currentUserId);
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (!BookingStatusEnum.PENDING.name().equals(booking.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Only pending booking requests can be handled");
        }
        return booking;
    }

    private void validateBookingOwnershipForSpecialist(Long bookingId, Long currentUserId) {
        Long count = bookingMapper.countBookingOwnedBySpecialist(bookingId, currentUserId);
        if (count == null || count == 0) {
            Booking booking = bookingMapper.selectById(bookingId);
            if (booking == null) {
                throw new BusinessException(ResultCodeEnum.NOT_FOUND);
            }
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
    }

    private static LocalDateTime resolveSlotStart(TimeSlot slot) {
        LocalDate date = slot.getSlotDate();
        LocalTime start = slot.getStartTime();
        if (date == null || start == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Time slot has no start time");
        }
        return LocalDateTime.of(date, start);
    }

    private <T> Optional<T> readCache(String key, Class<T> expectedType) {
        try {
            Object cacheValue = jsonRedisTemplate.opsForValue().get(key);
            if (cacheValue == null || !expectedType.isInstance(cacheValue)) {
                return Optional.empty();
            }
            return Optional.of(expectedType.cast(cacheValue));
        } catch (Exception exception) {
            log.warn("Redis read failed, fallback to database: key={}, reason={}", key, exception.getMessage());
            return Optional.empty();
        }
    }

    private void writeCache(String key, Object value, long ttlSeconds) {
        try {
            jsonRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            log.warn("Redis write failed, continue without cache: key={}, reason={}", key, exception.getMessage());
        }
    }

    private void invalidateCustomerBookingCache(Long customerId) {
        String cachePattern = RedisKeyUtils.buildCustomerBookingKeyPattern(customerId);
        try {
            Set<String> keys = jsonRedisTemplate.keys(cachePattern);
            if (keys != null && !keys.isEmpty()) {
                jsonRedisTemplate.delete(keys);
            }
        } catch (Exception exception) {
            log.warn("Redis cache invalidation failed, continue without cache eviction: pattern={}, reason={}", cachePattern, exception.getMessage());
        }
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

    private void validateDashboardDateRange(DashboardQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getStartDate() == null || queryDTO.getEndDate() == null) {
            return;
        }
        if (queryDTO.getStartDate().isAfter(queryDTO.getEndDate())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private List<DashboardStatisticsVO.TrendChartVO> loadTrendData(
            Long customerId,
            String completedStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (shouldUseDailyTrend(startDate, endDate)) {
            return Optional.ofNullable(
                            bookingMapper.selectDashboardTrendByDay(customerId, completedStatus, startDate, endDate)
                    )
                    .orElseGet(ArrayList::new);
        }
        return Optional.ofNullable(
                        bookingMapper.selectDashboardTrendByMonth(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(ArrayList::new);
    }

    private boolean shouldUseDailyTrend(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        long daySpan = endDate.toEpochDay() - startDate.toEpochDay();
        return daySpan <= DashboardConstant.TREND_DAILY_THRESHOLD_DAYS;
    }

    private List<DashboardStatisticsVO.HabitChartVO> buildHabitData(
            Long customerId,
            String completedStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<DashboardHabitRawVO> rawData = Optional.ofNullable(
                        bookingMapper.selectDashboardHabitData(customerId, completedStatus, startDate, endDate)
                )
                .orElseGet(ArrayList::new);

        Map<Integer, Integer> countByDayOfWeek = new HashMap<>();
        rawData.forEach(item -> {
            if (item.getDayOfWeek() != null) {
                countByDayOfWeek.put(item.getDayOfWeek(), Optional.ofNullable(item.getCount()).orElse(CommonConstant.NO));
            }
        });

        List<DashboardStatisticsVO.HabitChartVO> result = new ArrayList<>();
        for (int index = CommonConstant.NO; index < DashboardConstant.WEEKDAY_ORDER.size(); index++) {
            DashboardStatisticsVO.HabitChartVO habitChartVO = new DashboardStatisticsVO.HabitChartVO();
            habitChartVO.setDayOfWeek(DashboardConstant.WEEKDAY_ORDER.get(index));
            habitChartVO.setCount(countByDayOfWeek.getOrDefault(toMysqlDayOfWeek(index), CommonConstant.NO));
            result.add(habitChartVO);
        }
        return result;
    }

    private int toMysqlDayOfWeek(int weekdayOrderIndex) {
        if (weekdayOrderIndex == DashboardConstant.WEEKDAY_ORDER.size() - 1) {
            return 1;
        }
        return weekdayOrderIndex + 2;
    }
}

package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.constant.CommonConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.constant.DashboardConstant;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardHabitRawVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private void validateDashboardDateRange(DashboardQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getStartDate() == null || queryDTO.getEndDate() == null) {
            return;
        }
        if (queryDTO.getStartDate().isAfter(queryDTO.getEndDate())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private List<DashboardStatisticsVO.TrendChartVO> loadTrendData(Long customerId, String completedStatus, LocalDate startDate, LocalDate endDate) {
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

    private List<DashboardStatisticsVO.HabitChartVO> buildHabitData(Long customerId, String completedStatus, LocalDate startDate, LocalDate endDate) {
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

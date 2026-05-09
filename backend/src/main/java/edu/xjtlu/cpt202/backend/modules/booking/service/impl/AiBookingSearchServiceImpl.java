package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.AiBookingSearchService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.List;


/**
 *
 * @author QiranXiao
 * @since 2026/4/17
 */
@Service
public class AiBookingSearchServiceImpl implements AiBookingSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final Set<String> TIME_RANGE_VALUES = Set.of(
            "TODAY",
            "THIS_WEEK",
            "THIS_MONTH",
            "LAST_MONTH",
            "UPCOMING",
            "HISTORY"
    );

    private final BookingMapper bookingMapper;

    public AiBookingSearchServiceImpl(BookingMapper bookingMapper) {
        this.bookingMapper = bookingMapper;
    }

    @Override
    public AiBookingSearchResultVO searchCustomerBookings(Long customerId, AiBookingSearchDTO queryDTO) {
        if (customerId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        AiBookingSearchDTO normalizedQuery = normalizeQuery(queryDTO);
        LocalDateTime currentTime = LocalDateTime.now();
        ResolvedTimeFilter resolvedTimeFilter = resolveTimeFilter(normalizedQuery, currentTime);

        List<AiBookingSearchItemVO> items = bookingMapper.selectAiBookingSearchList(
                customerId,
                normalizedQuery,
                resolvedTimeFilter.startDate(),
                resolvedTimeFilter.endDate(),
                currentTime,
                resolvedTimeFilter.useUpcomingTimeFilter(),
                resolvedTimeFilter.useHistoryTimeFilter(),
                resolvedTimeFilter.sortAscending(),
                DEFAULT_LIMIT
        );
        Long totalMatched = bookingMapper.countAiBookingSearchList(
                customerId,
                normalizedQuery,
                resolvedTimeFilter.startDate(),
                resolvedTimeFilter.endDate(),
                currentTime,
                resolvedTimeFilter.useUpcomingTimeFilter(),
                resolvedTimeFilter.useHistoryTimeFilter()
        );

        return AiBookingSearchResultVO.builder()
                .totalMatched(totalMatched == null ? 0L : totalMatched)
                .returnedCount(items == null ? 0 : items.size())
                .items(items == null ? List.of() : items)
                .build();
    }

    private AiBookingSearchDTO normalizeQuery(AiBookingSearchDTO queryDTO) {
        AiBookingSearchDTO normalized = new AiBookingSearchDTO();
        if (queryDTO == null) {
            return normalized;
        }
        normalized.setExpertName(trimToNull(queryDTO.getExpertName()));
        normalized.setCategoryName(trimToNull(queryDTO.getCategoryName()));
        String normalizedStatus = normalizeUpperCase(queryDTO.getStatus());
        String normalizedTimeRangeType = normalizeUpperCase(queryDTO.getTimeRangeType());
        if (normalizedTimeRangeType == null && normalizedStatus != null && TIME_RANGE_VALUES.contains(normalizedStatus)) {
            normalizedTimeRangeType = normalizedStatus;
            normalizedStatus = null;
        }
        normalized.setStatus(normalizedStatus);
        normalized.setStartDate(queryDTO.getStartDate());
        normalized.setEndDate(queryDTO.getEndDate());
        normalized.setTimeRangeType(normalizedTimeRangeType);
        return normalized;
    }

    private ResolvedTimeFilter resolveTimeFilter(AiBookingSearchDTO queryDTO, LocalDateTime currentTime) {
        LocalDate startDate = queryDTO.getStartDate();
        LocalDate endDate = queryDTO.getEndDate();

        if (startDate != null || endDate != null) {
            validateDateRange(startDate, endDate);
            return new ResolvedTimeFilter(startDate, endDate, false, false, false);
        }

        String timeRangeType = queryDTO.getTimeRangeType();
        if (timeRangeType == null) {
            return new ResolvedTimeFilter(null, null, false, false, false);
        }

        LocalDate today = currentTime.toLocalDate();
        return switch (timeRangeType) {
            case "TODAY" -> new ResolvedTimeFilter(today, today, false, false, false);
            case "THIS_WEEK" -> new ResolvedTimeFilter(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                    false,
                    false,
                    false
            );
            case "THIS_MONTH" -> {
                YearMonth thisMonth = YearMonth.from(today);
                yield new ResolvedTimeFilter(thisMonth.atDay(1), thisMonth.atEndOfMonth(), false, false, false);
            }
            case "LAST_MONTH" -> {
                YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
                yield new ResolvedTimeFilter(lastMonth.atDay(1), lastMonth.atEndOfMonth(), false, false, false);
            }
            case "UPCOMING" -> new ResolvedTimeFilter(null, null, true, false, true);
            case "HISTORY" -> new ResolvedTimeFilter(null, null, false, true, false);
            default -> throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Unsupported timeRangeType");
        };
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpperCase(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private record ResolvedTimeFilter(
            LocalDate startDate,
            LocalDate endDate,
            boolean useUpcomingTimeFilter,
            boolean useHistoryTimeFilter,
            boolean sortAscending
    ) {
    }
}

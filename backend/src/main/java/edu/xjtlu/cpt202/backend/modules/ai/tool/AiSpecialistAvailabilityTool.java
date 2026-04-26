package edu.xjtlu.cpt202.backend.modules.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * AI tool for resolving a specialist name and date into concrete available slots.
 *
 * @author QiranXiao
 * @since 2026/4/25
 */
@Component
public class AiSpecialistAvailabilityTool {

    private static final int SEARCH_PAGE_SIZE = 8;

    private final SpecialistQueryService specialistQueryService;

    public AiSpecialistAvailabilityTool(SpecialistQueryService specialistQueryService) {
        this.specialistQueryService = specialistQueryService;
    }

    @Tool("""
            Search specialists by name or keyword and return their available slots on a date.
            Use this before preparing a booking confirmation draft when the user gives a specialist name, date, or time
            but does not provide specialistId and slotId. Use the returned specialistId and slotId with submitCurrentCustomerBooking.
            """)
    public SpecialistAvailabilitySearchResult searchSpecialistAvailabilityForBooking(
            @P("Specialist name or keyword, for example 'Emily Chen'. Required.") String specialistName,
            @P("Booking date in yyyy-MM-dd, for example 2026-04-28. Required.") LocalDate date
    ) {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setKeyword(specialistName);
        query.setDate(date);
        query.setPageNo(1);
        query.setPageSize(SEARCH_PAGE_SIZE);

        PageResult<SpecialistSummaryVO> pageResult = specialistQueryService.searchSpecialists(query);
        List<SpecialistAvailabilityItem> specialists = pageResult.getList().stream()
                .map(specialist -> toItem(specialist, date))
                .toList();

        return SpecialistAvailabilitySearchResult.builder()
                .success(true)
                .date(date)
                .total(pageResult.getTotal())
                .specialists(specialists)
                .build();
    }

    private SpecialistAvailabilityItem toItem(SpecialistSummaryVO specialist, LocalDate date) {
        List<AvailableSlotItem> availableSlots = specialistQueryService.listAvailability(specialist.getId(), date)
                .stream()
                .map(this::toSlotItem)
                .toList();

        return SpecialistAvailabilityItem.builder()
                .specialistId(specialist.getId())
                .specialistName(specialist.getName())
                .categoryName(specialist.getCategoryName())
                .consultationFee(specialist.getConsultationFee())
                .hasAvailabilityOnSelectedDate(specialist.getHasAvailabilityOnSelectedDate())
                .availableSlots(availableSlots)
                .build();
    }

    private AvailableSlotItem toSlotItem(SpecialistAvailabilityVO slot) {
        return AvailableSlotItem.builder()
                .slotId(slot.getId())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus())
                .build();
    }

    @Data
    @Builder
    public static class SpecialistAvailabilitySearchResult {
        private boolean success;
        private LocalDate date;
        private long total;
        private List<SpecialistAvailabilityItem> specialists;
    }

    @Data
    @Builder
    public static class SpecialistAvailabilityItem {
        private Long specialistId;
        private String specialistName;
        private String categoryName;
        private BigDecimal consultationFee;
        private Boolean hasAvailabilityOnSelectedDate;
        private List<AvailableSlotItem> availableSlots;
    }

    @Data
    @Builder
    public static class AvailableSlotItem {
        private Long slotId;
        private LocalDate slotDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status;
    }
}

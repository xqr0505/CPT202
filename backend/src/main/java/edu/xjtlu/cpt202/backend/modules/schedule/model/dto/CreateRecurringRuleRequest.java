package edu.xjtlu.cpt202.backend.modules.schedule.model.dto;

import edu.xjtlu.cpt202.backend.common.validation.ValidationGroups;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a recurring availability rule.
 * @author Schedule Module Team
 */
@Data
public class CreateRecurringRuleRequest {

    @NotNull(groups = ValidationGroups.Create.class, message = "Day of week is required")
    @Min(value = 1, message = "Day of week must be between 1 and 7")
    @Max(value = 7, message = "Day of week must be between 1 and 7")
    private Integer dayOfWeek;

    @NotNull(groups = ValidationGroups.Create.class, message = "Start time is required")
    private LocalTime startTime;

    @NotNull(groups = ValidationGroups.Create.class, message = "End time is required")
    private LocalTime endTime;

    @NotNull(groups = ValidationGroups.Create.class, message = "Effective end date is required")
    private LocalDate effectiveEndDate;
}

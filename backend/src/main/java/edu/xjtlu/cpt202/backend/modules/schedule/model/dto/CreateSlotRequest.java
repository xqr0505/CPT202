package edu.xjtlu.cpt202.backend.modules.schedule.model.dto;

import edu.xjtlu.cpt202.backend.common.validation.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a new time slot.
 * @author Schedule Module Team
 */
@Data
public class CreateSlotRequest {

    @NotNull(groups = ValidationGroups.Create.class, message = "Slot date is required")
    private LocalDate slotDate;

    @NotNull(groups = ValidationGroups.Create.class, message = "Start time is required")
    private LocalTime startTime;

    @NotNull(groups = ValidationGroups.Create.class, message = "End time is required")
    private LocalTime endTime;
}

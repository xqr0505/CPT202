package edu.xjtlu.cpt202.backend.modules.schedule.model.dto;

import lombok.Data;

import java.time.LocalTime;

/**
 * DTO for updating an existing time slot.
 * @author Schedule Module Team
 */
@Data
public class UpdateSlotRequest {

    private LocalTime startTime;

    private LocalTime endTime;

    private String status;
}

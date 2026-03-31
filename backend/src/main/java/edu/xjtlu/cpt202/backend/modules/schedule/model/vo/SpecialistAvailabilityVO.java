package edu.xjtlu.cpt202.backend.modules.schedule.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SpecialistAvailabilityVO {

    private Long id;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String status;
}

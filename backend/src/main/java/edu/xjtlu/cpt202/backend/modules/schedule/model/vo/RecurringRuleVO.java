package edu.xjtlu.cpt202.backend.modules.schedule.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class RecurringRuleVO {

    private Long id;

    private Long specialistId;

    private LocalDate effectiveStartDate;

    private Integer dayOfWeek;

    private String dayOfWeekDesc;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDate effectiveEndDate;

    private Integer isActive;

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

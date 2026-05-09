package edu.xjtlu.cpt202.backend.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
@Data
@TableName("availability_recurring_rules")
public class AvailabilityRecurringRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long specialistId;

    private LocalDate effectiveStartDate;

    private Integer dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDate effectiveEndDate;

    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}

package edu.xjtlu.cpt202.backend.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores per-occurrence overrides for recurring rules.
 * When a specialist manually changes or deletes one generated slot,
 * the original rule should not recreate that same occurrence later.
 */
@Data
@TableName("availability_recurring_rule_exceptions")
public class AvailabilityRecurringRuleException {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recurringRuleId;

    private LocalDate slotDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

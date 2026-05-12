package edu.xjtlu.cpt202.backend.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

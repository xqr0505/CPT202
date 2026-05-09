package edu.xjtlu.cpt202.backend.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.*;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@TableName("time_slots")
public class TimeSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long specialistId;

    private Long recurringRuleId;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String status;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

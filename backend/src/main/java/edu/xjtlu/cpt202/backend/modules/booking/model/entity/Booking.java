package edu.xjtlu.cpt202.backend.modules.booking.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bookings")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private Long specialistId;
    private Long slotId;
    private String status;
    private BigDecimal price;
    private String topic;
    private String customerNotes;
    private Long parentBookingId;
    private String rejectionReason;
    private LocalDateTime decisionTime;
    private String cancelledBy;
    private String cancelReason;
    private String changeType;
    private String refundStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


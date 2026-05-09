package edu.xjtlu.cpt202.backend.modules.booking.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author QiranXiao
 * @date 2026/4/1
 */
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

    public Booking() {
    }

    public Booking(Long id, Long customerId, Long specialistId, Long slotId, String status, BigDecimal price,
                   String topic, String customerNotes, Long parentBookingId, String rejectionReason,
                   LocalDateTime decisionTime, String cancelledBy, String cancelReason, String changeType,
                   String refundStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.specialistId = specialistId;
        this.slotId = slotId;
        this.status = status;
        this.price = price;
        this.topic = topic;
        this.customerNotes = customerNotes;
        this.parentBookingId = parentBookingId;
        this.rejectionReason = rejectionReason;
        this.decisionTime = decisionTime;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.changeType = changeType;
        this.refundStatus = refundStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSpecialistId() {
        return specialistId;
    }

    public void setSpecialistId(Long specialistId) {
        this.specialistId = specialistId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public Long getParentBookingId() {
        return parentBookingId;
    }

    public void setParentBookingId(Long parentBookingId) {
        this.parentBookingId = parentBookingId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getDecisionTime() {
        return decisionTime;
    }

    public void setDecisionTime(LocalDateTime decisionTime) {
        this.decisionTime = decisionTime;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


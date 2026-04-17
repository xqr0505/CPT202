package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author QiranXiao
 * @date 2026/3/31
 *
 */
@Schema(description = "Data Transfer Object for querying booking history list")
public class BookingHistoryQueryDTO {

    @Schema(description = "Page number", example = "1", defaultValue = "1")
    private Integer pageNo = 1;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "Time scope (UPCOMING or HISTORY) to filter bookings by time", example = "UPCOMING")
    private String timeScope;

    @Schema(description = "Booking status filter (e.g., COMPLETED, CANCELLED)", example = "COMPLETED")
    private String status;

    public BookingHistoryQueryDTO() {
    }

    public BookingHistoryQueryDTO(Integer pageNo, Integer pageSize, String timeScope, String status) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.timeScope = timeScope;
        this.status = status;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getTimeScope() {
        return timeScope;
    }

    public void setTimeScope(String timeScope) {
        this.timeScope = timeScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

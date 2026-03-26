package edu.xjtlu.cpt202.backend.common.result;

/**
 * Unified result code definitions for API responses.
 * Each enum value represents a specific business or system status.
 * @author QiranXiao
 * @date 2026/3/26
 */
public enum ResultCodeEnum {

    /**
     * Request succeeded.
     */
    SUCCESS(200, "Success"),
    /**
     * Request failed due to invalid parameters or client error.
     */
    BAD_REQUEST(400, "Bad request"),
    /**
     * Request failed due to missing or invalid authentication.
     */
    UNAUTHORIZED(401, "Unauthorized"),
    /**
     * Request failed due to insufficient permissions.
     */
    FORBIDDEN(403, "Forbidden"),
    /**
     * Internal server error or unexpected system exception.
     */
    SYSTEM_ERROR(500, "System error"),

    /**
     * Booking not found in the system (Module 6 specific).
     */
    BOOKING_NOT_FOUND(60001, "Booking not found"),
    /**
     * Booking status is invalid for the requested operation (Module 6 specific).
     */
    BOOKING_STATUS_INVALID(60002, "Invalid booking status"),
    /**
     * Booking conflict detected (e.g., time slot already taken, Module 6 specific).
     */
    BOOKING_CONFLICT(60003, "Booking conflict"),

    /**
     * Duplicate request detected (e.g., repeated submission within a short time).
     */
    DUPLICATE_REQUEST(90001, "Duplicate request");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

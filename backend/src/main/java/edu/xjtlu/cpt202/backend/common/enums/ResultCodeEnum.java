package edu.xjtlu.cpt202.backend.common.enums;

/**
 * Unified result code definitions for API responses.
 * Each enum value represents a specific business or system status.
 * @author QiranXiao
 * @date 2026/3/26
 */
public enum ResultCodeEnum implements BaseEnum<Integer> {

    /**
     * Request succeeded.
     */
    SUCCESS(200, "Success"),
    /**
     * Request failed due to invalid parameters or client error.
     */
    PARAM_ERROR(400, "Invalid parameters"),
    /**
     * Request failed due to missing or invalid authentication.
     */
    UNAUTHORIZED(401, "Please login first or token expired"),
    /**
     * Request failed due to insufficient permissions.
     */
    FORBIDDEN(403, "No permission to access this resource"),
    /**
     * Resource not found error.
     */
    NOT_FOUND(404, "Resource not found"),

    /**
     * Internal server error or unexpected system exception.
     */
    SYSTEM_ERROR(500, "Internal server error"),

    /**
     * Bad request error.
     */
    BAD_REQUEST(400, "Bad Request"),
    /**
     * Duplicate request error.
     */
    DUPLICATE_REQUEST(429, "Duplicate request"),

    /**
     * Authentication error block starting range.
     */
    AUTH_ERROR_BLOCK(1001, "Invalid Verification Code"),
    /**
     * User related error block starting range.
     */
    USER_ERROR_BLOCK(2001, "Account Locked"),
    /**
     * Booking related error block starting range.
     */
    BOOKING_ERROR_BLOCK(6001, "Time slot already booked");

    /**
     * The numeric code for the result status.
     */
    private final Integer code;
    /**
     * The description/message for the result status.
     */
    private final String desc;

    ResultCodeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return desc;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}

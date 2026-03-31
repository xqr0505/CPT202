package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

/**
 * Booking Status Enum.
 * Represents the core lifecycle of a booking.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum BookingStatusEnum implements BaseEnum<Integer> {
    /**
     * Pending approval from specialist
     */
    PENDING(0, "Pending approval from specialist"),
    /**
     * Approved by specialist, waiting to happen
     */
    CONFIRMED(1, "Approved by specialist, waiting to happen"),
    /**
     * Consultation finished successfully.
     */
    COMPLETED(2, "Consultation finished successfully"),
    /**
     * Cancelled by either party
     */
    CANCELLED(3, "Cancelled by either party");

    private final Integer code;
    private final String desc;

    BookingStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}


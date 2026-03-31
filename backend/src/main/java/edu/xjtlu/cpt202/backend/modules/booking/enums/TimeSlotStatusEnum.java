package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

/**
 * Time Slot Status Enum.
 * Used for Calendar and Slot Picker.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum TimeSlotStatusEnum implements BaseEnum<Integer> {
    /**
     * Open for booking.
     */
    AVAILABLE(0, "Open for booking"),
    /**
     * Successfully booked (Confirmed).
     */
    BOOKED(1, "Successfully booked (Confirmed)"),
    /**
     * Temporarily locked (Pending request in progress).
     */
    LOCKED(2, "Temporarily locked (Pending request in progress)");

    private final Integer code;
    private final String desc;

    TimeSlotStatusEnum(Integer code, String desc) {
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


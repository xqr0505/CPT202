package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

public enum TimeSlotStatusEnum implements BaseEnum<Integer> {
    AVAILABLE(0, "Open for booking"),
    BOOKED(1, "Successfully booked (Confirmed)"),
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


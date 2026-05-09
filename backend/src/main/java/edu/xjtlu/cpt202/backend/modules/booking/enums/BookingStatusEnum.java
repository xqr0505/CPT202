package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

public enum BookingStatusEnum implements BaseEnum<Integer> {
    PENDING(0, "Pending approval from specialist"),
    CONFIRMED(1, "Approved by specialist, waiting to happen"),
    COMPLETED(2, "Consultation finished successfully"),
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


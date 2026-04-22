package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

public enum BookingCancellationSourceEnum implements BaseEnum<String> {
    
    CUSTOMER_MANUAL("CUSTOMER_MANUAL", "Cancelled manually by customer"),
    
    SPECIALIST_MANUAL("SPECIALIST_MANUAL", "Cancelled manually by specialist"),
    
    SYSTEM_TIMEOUT("SYSTEM_TIMEOUT", "Cancelled automatically by system timeout");

    private final String code;
    private final String desc;

    BookingCancellationSourceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}

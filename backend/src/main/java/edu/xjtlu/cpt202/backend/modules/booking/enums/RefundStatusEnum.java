package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

public enum RefundStatusEnum implements BaseEnum<Integer> {
    NO_REFUND(0, "No refund required / Not eligible"),
    PENDING(1, "Waiting for admin/system to process refund"),
    PROCESSED(2, "Refund successfully returned to customer"),
    PENALTY_APPLIED(3, "Partial refund due to late cancellation penalty");

    private final Integer code;
    private final String desc;

    RefundStatusEnum(Integer code, String desc) {
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


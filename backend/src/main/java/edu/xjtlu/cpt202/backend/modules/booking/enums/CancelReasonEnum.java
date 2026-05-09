package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

/**
 *  Cancel Reason Enum.
 *  @author QiranXiao
 *  @date 2026/3/31
 */
public enum CancelReasonEnum implements BaseEnum<Integer> {
    /**
     * Customer personal reasons.
     */
    BY_CUSTOMER_PERSONAL(1, "Customer personal reasons"),
    /**
     * Customer schedule conflict.
     */
    BY_CUSTOMER_SCHEDULE_CONFLICT(2, "Customer schedule conflict"),
    /**
     * Specialist emergency / force majeure.
     */
    BY_SPECIALIST_FORCE_MAJEURE(3, "Specialist emergency / force majeure"),
    /**
     * Customer did not show up.
     */
    NO_SHOW(4, "Customer did not show up");

    private final Integer code;
    private final String desc;

    CancelReasonEnum(Integer code, String desc) {
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


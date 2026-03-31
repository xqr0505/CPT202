package edu.xjtlu.cpt202.backend.modules.booking.enums;

import edu.xjtlu.cpt202.backend.common.enums.BaseEnum;

/**
 * Refund Status Enum.
 * Used for Financial Tracking.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum RefundStatusEnum implements BaseEnum<Integer> {
    /**
     * No refund required / Not eligible.
     */
    NO_REFUND(0, "No refund required / Not eligible"),
    /**
     * Waiting for admin/system to process refund.
     */
    PENDING(1, "Waiting for admin/system to process refund"),
    /**
     * Refund successfully returned to customer.
     */
    PROCESSED(2, "Refund successfully returned to customer"),
    /**
     * Partial refund due to late cancellation penalty.
     */
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


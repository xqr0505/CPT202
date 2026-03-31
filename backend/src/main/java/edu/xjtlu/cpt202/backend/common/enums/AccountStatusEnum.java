package edu.xjtlu.cpt202.backend.common.enums;

/**
 * User account status definitions.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum AccountStatusEnum implements BaseEnum<Integer> {
    /**
     * Normal and active account.
     */
    ACTIVE(1, "Normal and active"),
    /**
     * Account locked due to multiple failed login attempts.
     */
    LOCKED(2, "Locked due to failed logins"),
    /**
     * Account deactivated by the user or an administrator.
     */
    DEACTIVATED(3, "Deactivated by user or admin");

    /**
     * The numeric code for the account status.
     */
    private final Integer code;
    /**
     * The description of the account status.
     */
    private final String desc;

    AccountStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}

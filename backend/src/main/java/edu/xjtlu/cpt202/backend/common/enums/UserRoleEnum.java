package edu.xjtlu.cpt202.backend.common.enums;

/**
 * User role definitions.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum UserRoleEnum implements BaseEnum<Integer> {
    /**
     * System Administrator.
     */
    ADMIN(1, "Administrator"),
    /**
     * Professional Specialist.
     */
    SPECIALIST(2, "Specialist"),
    /**
     * Regular Customer.
     */
    CUSTOMER(3, "Customer");

    /**
     * The numeric code for the user role.
     */
    private final Integer code;
    /**
     * The description of the user role.
     */
    private final String desc;

    UserRoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}

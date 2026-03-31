package edu.xjtlu.cpt202.backend.common.enums;

/**
 * Specialist level definitions.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum SpecialistLevelEnum implements BaseEnum<Integer> {
    /**
     * Junior Specialist.
     */
    JUNIOR(1, "Junior Specialist"),
    /**
     * Intermediate Specialist.
     */
    INTERMEDIATE(2, "Intermediate Specialist"),
    /**
     * Senior Specialist.
     */
    SENIOR(3, "Senior Specialist"),
    /**
     * Chief Specialist.
     */
    CHIEF(4, "Chief Specialist");

    /**
     * The numeric code for the specialist level.
     */
    private final Integer code;
    /**
     * The description of the specialist level.
     */
    private final String desc;

    SpecialistLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getDesc() { return desc; }
}

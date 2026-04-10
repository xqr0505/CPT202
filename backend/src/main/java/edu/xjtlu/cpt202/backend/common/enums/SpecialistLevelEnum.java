package edu.xjtlu.cpt202.backend.common.enums;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Specialist level definitions.
 * @author QiranXiao
 * @date 2026/3/31
 */
public enum SpecialistLevelEnum implements BaseEnum<Integer> {
    /**
     * Junior Specialist.
     */
    JUNIOR(1, "Junior Specialist", new BigDecimal("115.00"), new BigDecimal("135.00")),
    /**
     * Intermediate Specialist.
     */
    INTERMEDIATE(2, "Intermediate Specialist", new BigDecimal("165.00"), new BigDecimal("190.00")),
    /**
     * Senior Specialist.
     */
    SENIOR(3, "Senior Specialist", new BigDecimal("215.00"), new BigDecimal("240.00")),
    /**
     * Chief Specialist.
     */
    CHIEF(4, "Chief Specialist", new BigDecimal("255.00"), new BigDecimal("290.00"));

    /**
     * The numeric code for the specialist level.
     */
    private final Integer code;
    /**
     * The description of the specialist level.
     */
    private final String desc;
    /**
     * Minimum consultation fee for the level.
     */
    private final BigDecimal minFee;
    /**
     * Maximum consultation fee for the level.
     */
    private final BigDecimal maxFee;

    SpecialistLevelEnum(Integer code, String desc, BigDecimal minFee, BigDecimal maxFee) {
        this.code = code;
        this.desc = desc;
        this.minFee = minFee;
        this.maxFee = maxFee;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getDesc() { return desc; }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public static SpecialistLevelEnum fromName(String level) {
        return Arrays.stream(values())
                .filter(item -> item.name().equals(level))
                .findFirst()
                .orElse(null);
    }
}

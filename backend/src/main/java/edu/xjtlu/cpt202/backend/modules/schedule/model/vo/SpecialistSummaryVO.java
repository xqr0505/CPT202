package edu.xjtlu.cpt202.backend.modules.schedule.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecialistSummaryVO {

    private Long id;

    private Long userId;

    private String name;

    private String avatarUrl;

    private Long categoryId;

    private String categoryName;

    private String level;

    private BigDecimal consultationFee;

    private String bio;

    private String status;

    private Boolean hasAvailabilityOnSelectedDate;
}

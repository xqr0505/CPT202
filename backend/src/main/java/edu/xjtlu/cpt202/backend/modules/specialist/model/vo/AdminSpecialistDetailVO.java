package edu.xjtlu.cpt202.backend.modules.specialist.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSpecialistDetailVO {

    private Long id;

    private String name;

    private String avatarUrl;

    private Long categoryId;

    private String categoryName;

    private String level;

    private BigDecimal consultationFee;

    private String status;
}

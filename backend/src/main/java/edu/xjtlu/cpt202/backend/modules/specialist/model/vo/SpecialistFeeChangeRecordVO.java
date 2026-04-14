package edu.xjtlu.cpt202.backend.modules.specialist.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpecialistFeeChangeRecordVO {

    private Long id;

    private Long specialistId;

    private BigDecimal oldFee;

    private BigDecimal newFee;

    private String level;

    private BigDecimal rangeMin;

    private BigDecimal rangeMax;

    private Boolean outOfRange;

    private Long changedByUserId;

    private String changedByName;

    private LocalDateTime createdAt;
}

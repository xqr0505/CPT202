package edu.xjtlu.cpt202.backend.modules.specialist.model.vo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpecialistLevelOptionVO {

    private String value;

    private String label;

    private BigDecimal minFee;

    private BigDecimal maxFee;
}

package edu.xjtlu.cpt202.backend.modules.specialist.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("specialist_fee_change_records")
public class SpecialistFeeChangeRecord {

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

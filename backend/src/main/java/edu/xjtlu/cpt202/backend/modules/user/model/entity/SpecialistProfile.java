package edu.xjtlu.cpt202.backend.modules.user.model.entity;

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

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("specialist_profiles")
public class SpecialistProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long categoryId;
    private String level;
    private BigDecimal consultationFee;
    private String avatarUrl;
    private String bio;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


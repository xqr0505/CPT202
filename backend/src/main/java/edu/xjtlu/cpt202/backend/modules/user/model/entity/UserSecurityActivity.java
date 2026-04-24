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

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_security_activities")
public class UserSecurityActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String activityType;

    private String description;

    private String changedFields;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

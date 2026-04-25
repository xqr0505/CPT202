package edu.xjtlu.cpt202.backend.modules.user.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserSecurityActivityVO {

    private Long id;

    private String activityType;

    private String description;

    private List<String> changedFields;

    private LocalDateTime createdAt;
}

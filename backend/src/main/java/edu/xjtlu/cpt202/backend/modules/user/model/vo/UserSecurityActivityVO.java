package edu.xjtlu.cpt202.backend.modules.user.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSecurityActivityVO {

    private Long id;

    private String eventType;

    private String summary;

    private LocalDateTime createdAt;
}

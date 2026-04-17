package edu.xjtlu.cpt202.backend.modules.specialist.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminSpecialistListVO {

    private Long id;

    private String name;

    private String avatarUrl;

    private Long categoryId;

    private String categoryName;

    private String level;

    private BigDecimal consultationFee;

    private String status;

    private Boolean hasActiveBookings;

    private Integer activeBookingCount;

    private LocalDateTime createTime;
}

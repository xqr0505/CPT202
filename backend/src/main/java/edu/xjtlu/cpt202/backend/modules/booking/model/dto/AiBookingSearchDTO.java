package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 *
 * @author QiranXiao
 * @since 2026/4/17
 */
@Data
public class AiBookingSearchDTO {

    private String expertName;

    private String categoryName;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private String timeRangeType;
}

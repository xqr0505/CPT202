package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AiBookingSearchDTO {

    private String expertName;

    private String categoryName;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private String timeRangeType;
}

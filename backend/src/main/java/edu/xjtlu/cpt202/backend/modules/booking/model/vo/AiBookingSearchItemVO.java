package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

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
public class AiBookingSearchItemVO {

    private String bookingId;

    private String specialistId;

    private String specialistName;

    private String specialistAvatar;

    private String categoryName;

    private String serviceName;

    private LocalDateTime appointmentDateTime;

    private String status;

    private BigDecimal amount;
}

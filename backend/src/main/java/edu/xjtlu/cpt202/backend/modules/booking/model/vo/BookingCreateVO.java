package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Create booking response")
public class BookingCreateVO {

    @Schema(description = "Booking ID", example = "101")
    private Long bookingId;

    @Schema(description = "Booking status", example = "PENDING")
    private String status;
}

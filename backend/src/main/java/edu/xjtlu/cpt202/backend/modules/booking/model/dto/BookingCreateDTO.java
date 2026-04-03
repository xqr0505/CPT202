package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Create booking request")
public class BookingCreateDTO {

    @NotNull(message = "specialistId is required")
    @Schema(description = "Specialist profile ID", example = "1")
    private Long specialistId;

    @NotNull(message = "slotId is required")
    @Schema(description = "Time slot ID", example = "12")
    private Long slotId;

    @NotBlank(message = "topic is required")
    @Schema(description = "Booking topic", example = "Career planning")
    private String topic;

    @Schema(description = "Customer notes", example = "I want to discuss internship choices.")
    private String customerNotes;
}

package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "topic must be at most 100 characters")
    @Schema(description = "Visit reason selected from booking topics", example = "Initial Consultation")
    private String topic;

    @Size(max = 500, message = "customerNotes must be at most 500 characters")
    @Schema(description = "Optional medical notes", example = "Intermittent chest pain for two weeks.")
    private String customerNotes;
}

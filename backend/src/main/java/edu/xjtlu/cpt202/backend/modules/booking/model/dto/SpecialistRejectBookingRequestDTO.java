package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class SpecialistRejectBookingRequestDTO {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 300, message = "Rejection reason must be at most 300 characters")
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

package edu.xjtlu.cpt202.backend.modules.booking.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SpecialistForceCancelBookingRequestDTO {

    @NotBlank(message = "Cancel reason is required")
    @Size(max = 300, message = "Cancel reason must be at most 300 characters")
    private String cancelReason;

    @NotNull(message = "releaseSlot is required")
    private Boolean releaseSlot;

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Boolean getReleaseSlot() {
        return releaseSlot;
    }

    public void setReleaseSlot(Boolean releaseSlot) {
        this.releaseSlot = releaseSlot;
    }
}

package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmCurrentPasswordDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;
}

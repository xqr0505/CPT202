package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeCurrentUserEmailDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New email is required.")
    @Email(message = "Please enter a valid email address.")
    private String newEmail;

    @NotBlank(message = "Verification code is required.")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits.")
    private String code;
}

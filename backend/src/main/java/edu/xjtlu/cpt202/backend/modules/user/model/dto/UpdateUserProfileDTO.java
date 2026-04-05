package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserProfileDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\d{11}$",
            message = "Phone number must be exactly 11 digits"
    )
    private String phoneNumber;
}

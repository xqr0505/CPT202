package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserProfileDTO {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(
            regexp = "^\\+\\d{1,3}\\s\\d{4,14}$",
            message = "Please enter a valid phone number."
    )
    private String phoneNumber;
}

package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileDTO {

    @Size(max = 60, message = "Full name must be at most 60 characters.")
    private String fullName;

    @Email(message = "Please enter a valid email address.")
    private String email;

    @Size(max = 18, message = "Phone number must be at most 18 characters.")
    private String phoneNumber;
}

package edu.xjtlu.cpt202.backend.modules.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendChangeEmailCodeDTO {

    @NotBlank(message = "New email is required.")
    @Email(message = "Please enter a valid email address.")
    private String newEmail;
}

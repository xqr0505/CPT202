package edu.xjtlu.cpt202.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Verification code is required")
    private String verificationCode;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Role is required")
    private String role;
}

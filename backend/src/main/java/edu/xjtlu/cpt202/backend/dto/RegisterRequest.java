package edu.xjtlu.cpt202.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注册请求 DTO
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Verification code cannot be empty")
    private String verificationCode;

    @NotBlank(message = "Password cannot be empty")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters with uppercase, lowercase, and number"
    )
    private String password;

    @NotBlank(message = "Confirm password cannot be empty")
    private String confirmPassword;

    @NotBlank(message = "Role cannot be empty")
    private String role; // CUSTOMER, SPECIALIST
}

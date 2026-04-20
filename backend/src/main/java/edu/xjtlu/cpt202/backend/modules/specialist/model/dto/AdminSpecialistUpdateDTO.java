package edu.xjtlu.cpt202.backend.modules.specialist.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSpecialistUpdateDTO {

    @NotBlank(message = "name cannot be empty")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "email cannot be empty")
    @Email(message = "email format is invalid")
    @Size(max = 255, message = "email must be at most 255 characters")
    private String email;

    @Pattern(
            regexp = "^$|^.{8,64}$",
            message = "password length must be between 8 and 64 characters"
    )
    private String password;

    private Boolean resetPasswordToDefault;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotBlank(message = "level cannot be empty")
    @Size(max = 50, message = "level must be at most 50 characters")
    private String level;

    @NotNull(message = "consultationFee is required")
    @DecimalMin(value = "0.00", message = "consultationFee must be greater than or equal to 0")
    private BigDecimal consultationFee;

    @Pattern(
            regexp = "^(Active|Inactive)$",
            message = "status must be Active or Inactive"
    )
    @NotBlank(message = "status cannot be empty")
    private String status;

    @Size(max = 500, message = "avatarUrl must be at most 500 characters")
    private String avatarUrl;
}

package edu.xjtlu.cpt202.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class SendResetCodeRequest {
    @NotBlank @Email
    private String email;
}


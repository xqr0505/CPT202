package edu.xjtlu.cpt202.backend.modules.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}

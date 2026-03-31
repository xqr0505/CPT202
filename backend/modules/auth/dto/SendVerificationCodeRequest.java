package edu.xjtlu.cpt202.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码请求 DTO
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationCodeRequest {

    private String email;
    private String role;  // CUSTOMER, SPECIALIST（注册时需要）
    private String type;  // REGISTER, RESET_PASSWORD
}

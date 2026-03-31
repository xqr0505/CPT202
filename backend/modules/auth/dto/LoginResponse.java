package edu.xjtlu.cpt202.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO - 返回 Token 和用户信息
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;           // JWT Token
    private Long userId;            // 用户 ID
    private String role;            // 用户角色
    private String email;           // 用户邮箱
    private String displayName;     // 用户显示名称
    private Long expiresIn;         // Token 过期时间（秒）
}

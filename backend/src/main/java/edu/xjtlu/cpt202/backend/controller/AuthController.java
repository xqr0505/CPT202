package edu.xjtlu.cpt202.backend.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.dto.RegisterRequest;import edu.xjtlu.cpt202.backend.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.service.AuthService;
import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器 - 处理登录、注册、登出等认证相关业务
 * 
 * 工作流程：
 * 1. 用户提交登录信息 -> 验证密码 -> 生成 Token -> 返回给前端
 * 2. 前端保存 Token -> 后续请求自动添加到请求头
 * 3. 后端 JwtAuthenticationFilter 验证 Token -> 从 Token 中提取用户信息
 * 
 * @author DanyiHuang
 * @date 2026/3/29
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     * 
     * 请求示例：
     * POST /auth/login
     * {
     *   "email": "user@example.com",
     *   "password": "Password123",
     *   "role": "CUSTOMER"
     * }
     * 
     * 响应示例（成功）：
     * {
     *   "code": 200,
     *   "message": "Success",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "userId": 1,
     *     "role": "CUSTOMER",
     *     "email": "user@example.com",
     *     "displayName": "John Doe",
     *     "expiresIn": 1800
     *   }
     * }
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 用户注册
     * 
     * 请求示例：
     * POST /auth/register
     * {
     *   "email": "newuser@example.com",
     *   "verificationCode": "123456",
     *   "password": "Password123",
     *   "confirmPassword": "Password123",
     *   "role": "CUSTOMER"
     * }
     * 
     * 响应示例（成功）：
     * {
     *   "code": 200,
     *   "message": "Registration successful, login automatically",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "userId": 2,
     *     "role": "CUSTOMER",
     *     "email": "newuser@example.com",
     *     "displayName": "New User",
     *     "expiresIn": 1800
     *   }
     * }
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 发送验证码
     * 
     * 请求示例：
     * POST /auth/verify-email
     * {
     *   "email": "user@example.com",
     *   "role": "CUSTOMER",
     *   "type": "REGISTER"
     * }
     * 
     * 响应示例（成功）：
     * {
     *   "code": 200,
     *   "message": "Verification code sent successfully"
     * }
     * 
     * 错误示例：
     * {
     *   "code": 400,
     *   "message": "This email is already registered"
     * }
     */
    @PostMapping("/verify-email")
    public Result<Void> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return authService.sendVerificationCode(request);
    }

    /**
     * 用户登出
     * 
     * 请求示例：
     * POST /auth/logout
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     * 
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "Logout successful"
     * }
     * 
     * 注意：Token 是无状态的，服务器不需要黑名单操作
     * 前端只需要清除本地存储的 Token
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("User logout");

        // TODO: 可选 - 将 Token 加入黑名单以支持即时登出
        // 在实际项目中，如果需要立即失效已颁发的 Token：
        // 1. 可以使用 Redis 维护 Token 黑名单
        // 2. JwtAuthenticationFilter 验证 Token 时检查黑名单

        return Result.success();
    }

    /**
     * 密码重置请求
     * 
     * 请求示例：
     * POST /auth/reset-password
     * {
     *   "email": "user@example.com",
     *   "verificationCode": "123456",
     *   "newPassword": "NewPassword123",
     *   "confirmPassword": "NewPassword123"
     * }
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(
            @RequestParam String email,
            @RequestParam String verificationCode,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {

        log.info("Password reset request - Email: {}", email);

        // 验证两次密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            return Result.fail(400, "Passwords do not match");
        }

        return authService.resetPassword(email, verificationCode, newPassword);
    }
}

package edu.xjtlu.cpt202.backend.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
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
public class AuthController {

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
        log.info("User login attempt - Email: {}, Role: {}", request.getEmail(), request.getRole());

        // TODO: 这里需要实现真实的登录逻辑
        // 1. 根据 email 和 role 从数据库查询用户
        // 2. 验证密码是否正确
        // 3. 检查账户是否被锁定（登错密码5次15分钟内锁定）
        // 4. 如果通过验证，生成 Token

        // 示例响应（实际需要从数据库获取）
        Long userId = 1L;
        String role = "CUSTOMER";

        // 生成 JWT Token
        String token = JwtUtils.generateToken(userId, role);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(userId);
        response.setRole(role);
        response.setEmail(request.getEmail());
        response.setDisplayName("User");
        response.setExpiresIn(30 * 60L); // 30 minutes

        return Result.success(response);
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
        log.info("User registration attempt - Email: {}, Role: {}", request.getEmail(), request.getRole());

        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error(400, "Passwords do not match");
        }

        // TODO: 这里需要实现真实的注册逻辑
        // 1. 验证验证码是否正确且未过期
        // 2. 检查邮箱是否已注册
        // 3. 对密码进行加密（使用 BCryptPasswordEncoder）
        // 4. 保存用户到数据库
        // 5. 生成 Token 并自动登录

        // 示例响应
        Long userId = 2L;

        // 生成 JWT Token
        String token = JwtUtils.generateToken(userId, request.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(userId);
        response.setRole(request.getRole());
        response.setEmail(request.getEmail());
        response.setDisplayName("New User");
        response.setExpiresIn(30 * 60L);

        return Result.success(response);
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
    public Result<Void> sendVerificationCode(
            @RequestParam String email,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "REGISTER") String type) {

        log.info("Verification code request - Email: {}, Type: {}", email, type);

        // TODO: 这里需要实现真实的验证码发送逻辑
        // 1. 检查邮箱格式是否正确
        // 2. 如果是注册类型，需要检查是否已注册
        // 3. 检查请求频率（60秒内不能重复发送）
        // 4. 生成 6 位数字验证码
        // 5. 保存验证码到 Redis（10分钟过期）
        // 6. 通过 Email Service 发送验证码

        return Result.success();
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
            return Result.error(400, "Passwords do not match");
        }

        // TODO: 实现真实的密码重置逻辑
        // 1. 验证验证码是否正确且未过期
        // 2. 对新密码进行加密
        // 3. 更新数据库中的用户密码
        // 4. 清除该用户保存的 "记住我" 信息

        return Result.success();
    }
}

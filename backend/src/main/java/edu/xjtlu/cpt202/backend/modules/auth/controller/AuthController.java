package edu.xjtlu.cpt202.backend.modules.auth.controller;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LogoutRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RefreshTokenRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RefreshTokenResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.ResetPasswordRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.VerifyResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/auth/verify-email", "/api/auth/verify-email"})
    public Result<Void> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request);
        return Result.success();
    }

    @PostMapping({"/auth/register", "/api/auth/register"})
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping({"/auth/login", "/api/auth/login"})
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping({"/auth/logout", "/api/auth/logout"})
    public Result<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request == null ? null : request);
        return Result.success();
    }

    @PostMapping({"/auth/refresh-token", "/api/auth/refresh-token"})
    public Result<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request));
    }

    @PostMapping({"/auth/reset-password/send-code", "/api/auth/reset-password/send-code"})
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeRequest request) {
        authService.sendResetPasswordCode(request);
        return Result.success();
    }

    @PostMapping({"/auth/reset-password/verify", "/api/auth/reset-password/verify"})
    public Result<Void> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        authService.verifyResetCode(request);
        return Result.success();
    }

    @PostMapping({"/auth/reset-password/update", "/api/auth/reset-password/update"})
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }
}

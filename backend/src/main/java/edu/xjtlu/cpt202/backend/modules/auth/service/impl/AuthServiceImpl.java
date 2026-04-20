package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.constant.SecurityConstant;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LogoutRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RefreshTokenRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RefreshTokenResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.ResetPasswordRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.VerifyResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.RefreshTokenMapper;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.RefreshToken;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import edu.xjtlu.cpt202.backend.modules.auth.service.VerificationCodeService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final String REGISTER_CODE_EMAIL_SUBJECT = "Email Verification";
    private static final String REGISTER_CODE_EMAIL_TEMPLATE =
            "Your verification code is: %s\nThis code will expire in %d minutes.";
    private static final String RESET_PASSWORD_CODE_EMAIL_SUBJECT = "Password Reset Verification";
    private static final String RESET_PASSWORD_CODE_EMAIL_TEMPLATE =
            "Your password reset verification code is: %s\nThis code will expire in %d minutes.\nIf you did not request this, please ignore this email.";

    private final UserMapper userMapper;
    private final UserAccountService userAccountService;
    private final VerificationCodeMapper verificationCodeMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public AuthServiceImpl(UserMapper userMapper,
                           UserAccountService userAccountService,
                           VerificationCodeMapper verificationCodeMapper,
                           RefreshTokenMapper refreshTokenMapper,
                           PasswordEncoder passwordEncoder,
                           VerificationCodeService verificationCodeService) {
        this.userMapper = userMapper;
        this.userAccountService = userAccountService;
        this.verificationCodeMapper = verificationCodeMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String type = request.getType();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        
        // 1. 根据类型执行不同校验
        if ("REGISTER".equals(type)) {
            String role = request.getRole();
            if (StrUtil.isBlank(role)) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role");
            }
            role = role.trim().toUpperCase(Locale.ROOT);
            if (!"CUSTOMER".equals(role)) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role first");
            }

            // 注册模式：邮箱必须未被注册
            Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
            if (existingCount != null && existingCount > 0) {
                throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
            }
        } else if ("RESET_PASSWORD".equals(type)) {
            // 重置密码模式：不需要角色，但邮箱必须已注册
            Long userExists = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
            if (userExists == null || userExists == 0) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Email is not registered.");
            }
        } else {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Invalid verification type");
        }
        
        if ("REGISTER".equals(type)) {
            verificationCodeService.sendCode(
                    email,
                    type,
                    REGISTER_CODE_EMAIL_SUBJECT,
                    REGISTER_CODE_EMAIL_TEMPLATE
            );
            return;
        }

        verificationCodeService.sendCode(
                email,
                type,
                RESET_PASSWORD_CODE_EMAIL_SUBJECT,
                RESET_PASSWORD_CODE_EMAIL_TEMPLATE
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 8/9/10 校验
        if (StrUtil.hasBlank(request.getEmail(), request.getVerificationCode(), request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please enter every field");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Password not meet complexity or password not match");
        }

        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Password not meet complexity or password not match");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String role = request.getRole();
        if (StrUtil.isBlank(role)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role");
        }
        role = role.trim().toUpperCase(Locale.ROOT);
        if (!"CUSTOMER".equals(role)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Only CUSTOMER role is allowed");
        }

        Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }
        VerificationCode codeRecord = verificationCodeService.requireLatestValidCode(
                email,
                "REGISTER",
                request.getVerificationCode(),
                "Verification code incorrect or expired. Please request a new one."
        );

        User newUser = userAccountService.createUser(email, request.getPassword(), "CUSTOMER", null);

        verificationCodeService.markCodeUsed(codeRecord);

        String token = JwtUtils.generateToken(newUser.getId(), newUser.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(newUser.getId())
                .role(newUser.getRole())
                .email(newUser.getEmail())
                .displayName(newUser.getFullName() != null ? newUser.getFullName() : newUser.getEmail())
                .expiresIn(System.currentTimeMillis() + SecurityConstant.JWT_EXPIRATION_MILLISECONDS)
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (StrUtil.hasBlank(request.getEmail(), request.getPassword(), request.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please enter every field");
        }

        String role = request.getRole().trim().toUpperCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("email", email));

        if (user == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid email or password");
        }

        if (!role.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "role not match");
        }

        if (AccountStatusEnum.LOCKED.name().equals(user.getStatus())) {
            // 检查锁定时间是否已过（如果 lockTime + 锁定分钟数 > 当前时间，则仍锁定）
            if (user.getLockTime() != null) {
                LocalDateTime unlockTime = user.getLockTime().plusMinutes(SecurityConstant.ACCOUNT_LOCK_DURATION_MINUTES);
                if (LocalDateTime.now().isBefore(unlockTime)) {
                    throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), 
                        "Too many failed attempts. Please try again after 15 minutes.");
                } else {
                    // 锁定已过期，自动解锁并重置失败计数
                    user.setStatus(AccountStatusEnum.ACTIVE.name());
                    user.setLoginFailCount(0);
                    user.setLockTime(null);
                    user.setFirstFailTime(null);
                    userMapper.updateById(user);
                }
            } else {
                // 没有 lockTime 但状态为 LOCKED，视为永久锁定（或直接抛异常）
                throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(),
                        "Too many failed attempts. Please try again after 15 minutes.");
            }
        }

        if (AccountStatusEnum.DEACTIVATED.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), "Account has been deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime firstFailTime = user.getFirstFailTime();
            int newFailCount;

            if (firstFailTime == null || firstFailTime.isBefore(now.minusMinutes(3))) {
                newFailCount = 1;
                user.setFirstFailTime(now);
            } else {
                newFailCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            }

            user.setLoginFailCount(newFailCount);
            if (newFailCount >= SecurityConstant.MAX_LOGIN_ATTEMPTS) {
                user.setStatus(AccountStatusEnum.LOCKED.name());
                user.setLockTime(now);
                userMapper.updateById(user);
                throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(),
                        "Too many failed attempts. Please try again after 15 minutes.");
            } else {
                userMapper.updateById(user);
                throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid email or password");
            }
        }

        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setFirstFailTime(null);
        // 如果你有 lastLoginTime 字段，可以在这里更新
        // user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String accessToken = JwtUtils.generateToken(user.getId(), user.getRole());
        String refreshToken = generateRefreshToken();
        persistRefreshToken(user.getId(), refreshToken);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(user.getRole())
                .email(user.getEmail())
                .displayName(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .expiresIn(System.currentTimeMillis() + SecurityConstant.JWT_EXPIRATION_MILLISECONDS)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenValue = request.getRefreshToken().trim();
        if (StrUtil.isBlank(refreshTokenValue)) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid refresh token");
        }

        RefreshToken persistedToken = refreshTokenMapper.selectOne(new QueryWrapper<RefreshToken>()
                .eq("token", refreshTokenValue)
                .ge("expires_at", LocalDateTime.now())
        );

        if (persistedToken == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Refresh token invalid or expired");
        }

        User user = userMapper.selectById(persistedToken.getUserId());
        if (user == null || !AccountStatusEnum.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "User session is invalid");
        }

        if (user.getPasswordChangedAt() != null && persistedToken.getCreatedAt() != null &&
                persistedToken.getCreatedAt().isBefore(user.getPasswordChangedAt())) {
            invalidateRefreshToken(refreshTokenValue);
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Refresh token invalid due to password change");
        }

        String accessToken = JwtUtils.generateToken(user.getId(), user.getRole());

        return RefreshTokenResponse.builder()
                .token(accessToken)
                .expiresIn(System.currentTimeMillis() + SecurityConstant.JWT_EXPIRATION_MILLISECONDS)
                .build();
    }

    @Override
    public void logout(LogoutRequest request) {
        if (request != null && StrUtil.isNotBlank(request.getRefreshToken())) {
            invalidateRefreshToken(request.getRefreshToken().trim());
        }
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID().toString();
    }

    private void persistRefreshToken(Long userId, String refreshTokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusDays(SecurityConstant.REFRESH_TOKEN_EXPIRATION_DAYS))
                .build();
        refreshTokenMapper.insert(refreshToken);
    }

    private void invalidateRefreshToken(String refreshTokenValue) {
        refreshTokenMapper.delete(new QueryWrapper<RefreshToken>().eq("token", refreshTokenValue));
    }

    private void invalidateAllRefreshTokensForUser(Long userId) {
        refreshTokenMapper.delete(new QueryWrapper<RefreshToken>().eq("user_id", userId));
    }

    @Override
    public void sendResetPasswordCode(SendResetCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        
        // 1. 检查邮箱是否已注册
        Long userExists = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
        if (userExists == null || userExists == 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), 
                "Email is not registered.");
        }
        
        verificationCodeService.sendCode(
                email,
                "RESET_PASSWORD",
                RESET_PASSWORD_CODE_EMAIL_SUBJECT,
                RESET_PASSWORD_CODE_EMAIL_TEMPLATE
        );
    }

    @Override
    public void verifyResetCode(VerifyResetCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String code = request.getVerificationCode().trim();
        
        VerificationCode codeRecord = verificationCodeService.requireLatestValidCode(
                email,
                "RESET_PASSWORD",
                code,
                "Invalid or expired verification code"
        );
        verificationCodeService.markCodeUsed(codeRecord);
        
        // 不返回任何数据，仅表示验证通过
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String verificationCode = request.getVerificationCode().trim();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();
        
        // 1. 校验非空（虽然 DTO 已有 @NotBlank，但防御性编程）
        if (StrUtil.hasBlank(email, verificationCode, newPassword, confirmPassword)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), 
                "All fields cannot be empty");
        }
        
        // 2. 校验密码一致性
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), 
                "Passwords do not match");
        }
        
        // 3. 校验密码复杂度
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), 
                "Password not meet complexity (at least 8 chars, uppercase, lowercase, and a number)");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 4. 验证验证码（再次查询数据库，确保未被使用且未过期）
        VerificationCode codeRecord = verificationCodeMapper.selectOne(
            new QueryWrapper<VerificationCode>()
                .eq("email", email)
                .eq("type", "RESET_PASSWORD")
                .eq("code", verificationCode)
                .eq("is_used", true)          // 因为 verify 步骤已经标记为已使用，这里查 true
                .ge("expires_at", now)
                .orderByDesc("created_at")
                .last("LIMIT 1")
        );
        
        // 如果 verify 步骤未标记已使用，则改为 is_used = false 并增加过期检查
        // 为了安全，我们允许两种方式：若 verify 没有标记，这里仍然可以处理
        if (codeRecord == null) {
            // 尝试查询未使用的记录（兼容 verify 未标记的情况）
            codeRecord = verificationCodeMapper.selectOne(
                new QueryWrapper<VerificationCode>()
                    .eq("email", email)
                    .eq("type", "RESET_PASSWORD")
                    .eq("code", verificationCode)
                    .eq("is_used", false)
                    .ge("expires_at", now)
                    .orderByDesc("created_at")
                    .last("LIMIT 1")
            );
            if (codeRecord == null) {
                throw new BusinessException(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), 
                    "Invalid or expired verification code");
            }
            // 标记为已使用
            codeRecord.setIsUsed(true);
            verificationCodeMapper.updateById(codeRecord);
        }
        
        // 5. 查找用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), 
                "Email is not registered");
        }
        
        // 6. 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        // 可选：重置失败计数和锁定状态（因为用户成功重置密码，应解除锁定）
        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setFirstFailTime(null);
        if (AccountStatusEnum.LOCKED.name().equals(user.getStatus())) {
            user.setStatus(AccountStatusEnum.ACTIVE.name());
        }
        userMapper.updateById(user);
        invalidateAllRefreshTokensForUser(user.getId());

        logger.info("Password reset successfully for email: {}", email);
    }
}

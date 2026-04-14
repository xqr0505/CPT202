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
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.ResetPasswordRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.VerifyResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserMapper userMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Environment env;

    @Autowired
    public AuthServiceImpl(UserMapper userMapper,
                           VerificationCodeMapper verificationCodeMapper,
                           PasswordEncoder passwordEncoder,
                           JavaMailSender mailSender,
                           Environment env) {
        this.userMapper = userMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.env = env;
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String type = request.getType();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        
        // 1. 根据类型执行不同校验
        if ("REGISTER".equals(type)) {
            // 注册模式：必须有角色，且角色合法
            if (StrUtil.isBlank(request.getRole())) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role first.");
            }
            String role = request.getRole().toUpperCase(Locale.ROOT);
            if (!"CUSTOMER".equals(role) && !"SPECIALIST".equals(role)) {
                throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role first.");
            }
            // 邮箱必须未被注册
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
        
        // 2. 冷却检查（按类型分开）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixtySecondsAgo = now.minusSeconds(SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS);
        Long recentCount = verificationCodeMapper.selectCount(
            new QueryWrapper<VerificationCode>()
                .eq("email", email)
                .eq("type", type)
                .gt("created_at", sixtySecondsAgo)
        );
        if (recentCount != null && recentCount > 0) {
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(),
                "Please wait " + SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS + " seconds");
        }
        
        // 3. 生成验证码并发送（后续代码不变）
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .type(type)
                .isUsed(false)
                .expiresAt(now.plusMinutes(SecurityConstant.VERIFICATION_CODE_EXPIRATION_MINUTES))
                .build();
        verificationCodeMapper.insert(verificationCode);

        try {
            if (mailSender == null) {
                logger.error("Mail sender not configured");
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                        "Mail service is unavailable");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                logger.error("Mail sender returned null MimeMessage");
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                        "Mail service is unavailable");
            }

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            String fromAddress = env == null ? null : env.getProperty("spring.mail.username");
            if (StrUtil.isBlank(fromAddress)) {
                fromAddress = "noreply@example.com";
            }

            helper.setFrom("ExpertLink <" + fromAddress + ">");
            helper.setTo(email);
            helper.setSubject("Email Verification");
            helper.setText("Your verification code is: " + code + "\nThis code will expire in 5 minutes.");

            mailSender.send(mimeMessage);

            logger.info("Verification email sent to {}", request.getEmail());

        } catch (Exception e) {
            logger.error("Failed to send verification email: {}", e.getMessage(), e);

            verificationCodeMapper.deleteById(verificationCode.getId());

            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Failed to send verification email");
        }

        logger.info("Verification code '{}' generated for {}", code, email);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 8/9/10 校验
        if (StrUtil.hasBlank(request.getEmail(), request.getVerificationCode(), request.getPassword(), request.getConfirmPassword(), request.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please enter every field");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Password not meet complexity or password not match");
        }

        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Password not meet complexity or password not match");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", email));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }
        LocalDateTime now = LocalDateTime.now();

        VerificationCode codeRecord = verificationCodeMapper.selectOne(
                new QueryWrapper<VerificationCode>()
                        .eq("email", email)
                        .eq("type", "REGISTER")
                        .eq("is_used", false)
                        .orderByDesc("created_at")
                        .last("LIMIT 1")
        );

        if (codeRecord == null || codeRecord.getExpiresAt() == null || codeRecord.getExpiresAt().isBefore(now) || !codeRecord.getCode().equals(request.getVerificationCode().trim())) {
            throw new BusinessException(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), "Verification code incorrect or expired. Please request a new one.");
        }

        String role = request.getRole().trim().toUpperCase(Locale.ROOT);
        if (!"CUSTOMER".equals(role) && !"SPECIALIST".equals(role)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Only CUSTOMER or SPECIALIST can register.");
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(AccountStatusEnum.ACTIVE.name())
                .loginFailCount(0)
                .lockTime(null)
                .build();

        userMapper.insert(newUser);

        codeRecord.setIsUsed(true);
        verificationCodeMapper.updateById(codeRecord);

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

        String token = JwtUtils.generateToken(user.getId(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole())
                .email(user.getEmail())
                .displayName(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .expiresIn(System.currentTimeMillis() + SecurityConstant.JWT_EXPIRATION_MILLISECONDS)
                .build();
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
        
        // 2. 冷却检查（针对 RESET_PASSWORD 类型，独立冷却）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixtySecondsAgo = now.minusSeconds(SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS);
        Long recentCount = verificationCodeMapper.selectCount(
            new QueryWrapper<VerificationCode>()
                .eq("email", email)
                .eq("type", "RESET_PASSWORD")
                .gt("created_at", sixtySecondsAgo)
        );
        if (recentCount != null && recentCount > 0) {
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(),
                "Please wait " + SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS + 
                " seconds before requesting a new code");
        }
        
        // 3. 生成 6 位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        
        // 4. 存储验证码记录
        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .type("RESET_PASSWORD")
                .isUsed(false)
                .expiresAt(now.plusMinutes(SecurityConstant.VERIFICATION_CODE_EXPIRATION_MINUTES))
                .build();
        verificationCodeMapper.insert(verificationCode);
        
        // 5. 发送邮件
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            
            String fromAddress = env.getProperty("spring.mail.username");
            if (StrUtil.isBlank(fromAddress)) {
                fromAddress = "noreply@example.com";
            }
            helper.setFrom("ExpertLink <" + fromAddress + ">");
            helper.setTo(email);
            helper.setSubject("Password Reset Verification");
            helper.setText("Your password reset verification code is: " + code + 
                        "\nThis code will expire in 5 minutes.\nIf you did not request this, please ignore this email.");
            
            mailSender.send(mimeMessage);
            logger.info("Password reset code sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email: {}", e.getMessage(), e);
            // 发送失败则删除刚插入的记录
            verificationCodeMapper.deleteById(verificationCode.getId());
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Failed to send verification email");
        }
    }

    @Override
    public void verifyResetCode(VerifyResetCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String code = request.getVerificationCode().trim();
        
        LocalDateTime now = LocalDateTime.now();
        
        // 查询最新的一条未使用、未过期的 RESET_PASSWORD 验证码
        VerificationCode codeRecord = verificationCodeMapper.selectOne(
            new QueryWrapper<VerificationCode>()
                .eq("email", email)
                .eq("type", "RESET_PASSWORD")
                .eq("is_used", false)
                .ge("expires_at", now)          // 未过期
                .orderByDesc("created_at")
                .last("LIMIT 1")
        );
        
        if (codeRecord == null || !codeRecord.getCode().equals(code)) {
            throw new BusinessException(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), 
                "Invalid verification code");
        }
        
        // 可选：标记为已使用，防止重复使用（也可以在 resetPassword 中标记）
        // 这里标记后，resetPassword 中就不需要再检查 is_used 了，但仍需检查 code 和 email
        codeRecord.setIsUsed(true);
        verificationCodeMapper.updateById(codeRecord);
        
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
        
        logger.info("Password reset successfully for email: {}", email);
    }
}

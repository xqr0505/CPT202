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
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private org.springframework.core.env.Environment env;

    @Autowired
    public AuthServiceImpl(UserMapper userMapper,
                           VerificationCodeMapper verificationCodeMapper) {
        this.userMapper = userMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        if (StrUtil.isBlank(request.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role first.");
        }

        String role = request.getRole().toUpperCase(Locale.ROOT);
        if (!"CUSTOMER".equals(role) && !"SPECIALIST".equals(role)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please select a role first.");
        }

        if (StrUtil.isBlank(request.getEmail())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Email is required");
        }

        // Email format is validated by @Email in DTO, but we keep defensive coding.
        if (!request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Invalid email format");
        }

        // 4: 邮箱已存在
        Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }

        LocalDateTime now = LocalDateTime.now();
        VerificationCode latestCode = verificationCodeMapper.selectOne(
                new QueryWrapper<VerificationCode>()
                        .eq("email", request.getEmail())
                        .eq("type", "REGISTER")
                        .orderByDesc("created_at")
                        .last("LIMIT 1")
        );

        if (latestCode != null && !Boolean.TRUE.equals(latestCode.getIsUsed())) {
            if (latestCode.getCreatedAt() != null && latestCode.getCreatedAt().isAfter(now.minusSeconds(SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS))) {
                throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), "Please wait 60 seconds before requesting a new code");
            }
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        VerificationCode verificationCode = VerificationCode.builder()
                .email(request.getEmail())
                .code(code)
                .type("REGISTER")
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
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

        helper.setFrom("ExpertLink <" + env.getProperty("spring.mail.username") + ">");
        helper.setTo(request.getEmail());
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

        logger.info("Verification code '{}' generated for {}", code, request.getEmail());
    }

    @Override
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

        Long existingCount = userMapper.selectCount(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "This email is already registered");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
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

        codeRecord.setIsUsed(true);
        verificationCodeMapper.updateById(codeRecord);

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
                .lockTime(now)
                .build();

        userMapper.insert(newUser);

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
        // 1. 基础字段非空校验（已存在）
        if (StrUtil.hasBlank(request.getEmail(), request.getPassword(), request.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Please enter every field");
        }

        String role = request.getRole().trim().toUpperCase(Locale.ROOT);
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("email", request.getEmail().trim().toLowerCase(Locale.ROOT))
                .eq("role", role));

        // 2. 用户不存在，统一返回“无效凭证”（防止枚举用户名）
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid credentials");
        }

        // 3. 检查账户是否被锁定
        if (AccountStatusEnum.LOCKED.name().equals(user.getStatus())) {
            // 检查锁定时间是否已过（如果 lockTime + 锁定分钟数 > 当前时间，则仍锁定）
            if (user.getLockTime() != null) {
                LocalDateTime unlockTime = user.getLockTime().plusMinutes(SecurityConstant.ACCOUNT_LOCK_DURATION_MINUTES);
                if (LocalDateTime.now().isBefore(unlockTime)) {
                    throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), 
                        "Account is locked, please try again later.");
                } else {
                    // 锁定已过期，自动解锁并重置失败计数
                    user.setStatus(AccountStatusEnum.ACTIVE.name());
                    user.setLoginFailCount(0);
                    user.setLockTime(null);
                    userMapper.updateById(user);
                }
            } else {
                // 没有 lockTime 但状态为 LOCKED，视为永久锁定（或直接抛异常）
                throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), "Account is locked.");
            }
        }

        // 4. 检查账户是否被注销
        if (AccountStatusEnum.DEACTIVATED.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), "Account has been deactivated.");
        }

        // 5. 密码校验
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 密码错误：增加失败计数
            int newFailCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(newFailCount);
            if (newFailCount >= SecurityConstant.MAX_LOGIN_ATTEMPTS) {
                // 锁定账户
                user.setStatus(AccountStatusEnum.LOCKED.name());
                user.setLockTime(LocalDateTime.now());
                userMapper.updateById(user);
                throw new BusinessException(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), 
                    "Too many failed attempts. Account locked for " + SecurityConstant.ACCOUNT_LOCK_DURATION_MINUTES + " minutes.");
            } else {
                userMapper.updateById(user);
                throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid credentials");
            }
        }

        // 6. 登录成功：重置失败计数、清空锁定时间、更新最后登录时间（如果有字段）
        user.setLoginFailCount(0);
        user.setLockTime(null);
        // 如果你有 lastLoginTime 字段，可以在这里更新
        // user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 7. 生成 JWT
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
}

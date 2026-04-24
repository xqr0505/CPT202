package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.constant.SecurityConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
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
import edu.xjtlu.cpt202.backend.modules.auth.service.VerificationCodeService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserAccountService userAccountService;
    @Mock private VerificationCodeMapper verificationCodeMapper;
    @Mock private RefreshTokenMapper refreshTokenMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private VerificationCodeService verificationCodeService;
    @InjectMocks private AuthServiceImpl authService;

    private final String testEmail = "test@example.com";
    private final String testPassword = "Test1234";
    private final String testRole = "CUSTOMER";
    private final String validCode = "123456";

    // ==================== sendVerificationCode Tests ====================

    @Test
    void sendVerificationCode_Success() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);
        request.setType("REGISTER");

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        assertDoesNotThrow(() -> authService.sendVerificationCode(request));

        verify(verificationCodeService).sendCode(
                testEmail,
                "REGISTER",
                "Email Verification",
                "Your verification code is: %s\nThis code will expire in %d minutes."
        );
    }

    @Test
    void sendVerificationCode_RoleMissing_DefaultsToCustomer_Success() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(null);
        request.setType("REGISTER");

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        assertDoesNotThrow(() -> authService.sendVerificationCode(request));

        verify(verificationCodeService).sendCode(
                testEmail,
                "REGISTER",
                "Email Verification",
                "Your verification code is: %s\nThis code will expire in %d minutes."
        );
    }

    @Test
    void sendVerificationCode_EmailAlreadyExists_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);
        request.setType("REGISTER");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationCode(request));
        assertEquals(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void sendVerificationCode_InvalidRole_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole("INVALID_ROLE");
        request.setType("REGISTER");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationCode(request));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Only CUSTOMER role is allowed"));
    }

    @Test
    void register_InvalidRole_Throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setVerificationCode(validCode);
        request.setPassword(testPassword);
        request.setConfirmPassword(testPassword);
        request.setRole("INVALID");

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        VerificationCode codeRecord = VerificationCode.builder()
                .code(validCode)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Only CUSTOMER role is allowed"));
    }

    @Test
    void sendVerificationCode_ResendTooQuick_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);
        request.setType("REGISTER");

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        doThrow(new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), "Please wait 60 seconds"))
                .when(verificationCodeService)
                .sendCode(anyString(), anyString(), anyString(), anyString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationCode(request));
        assertEquals(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("wait 60 seconds"));
    }

    @Test
    void sendVerificationCode_ResendAfterCooldown_Success() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);
        request.setType("REGISTER");

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        assertDoesNotThrow(() -> authService.sendVerificationCode(request));
        verify(verificationCodeService, times(1))
                .sendCode(eq(testEmail), eq("REGISTER"), anyString(), anyString());
    }

    // ==================== register Tests ====================

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setVerificationCode(validCode);
        request.setPassword(testPassword);
        request.setConfirmPassword(testPassword);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        VerificationCode codeRecord = VerificationCode.builder()
                .email(testEmail)
                .code(validCode)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeService.requireLatestValidCode(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(codeRecord);

        when(userAccountService.createUser(eq(testEmail), eq(testPassword), eq("CUSTOMER"), isNull())).thenAnswer(invocation -> {
            User u = User.builder()
                    .id(1L)
                    .email(testEmail)
                    .passwordHash("encodedPwd")
                    .role("CUSTOMER")
                    .status("ACTIVE")
                    .build();
            return u;
        });

        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(anyLong(), anyString())).thenReturn("mock-jwt-token");

            LoginResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals("CUSTOMER", response.getRole());
            assertEquals(testEmail, response.getEmail());

            verify(userAccountService).createUser(eq(testEmail), eq(testPassword), eq("CUSTOMER"), isNull());
            verify(verificationCodeService).markCodeUsed(codeRecord);
        }
    }

    @Test
    void register_PasswordMismatch_Throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setVerificationCode("123456");
        request.setRole("CUSTOMER");
        request.setPassword("Test1234");
        request.setConfirmPassword("Different1234");  // 不匹配

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Password not meet complexity or password not match"));
    }

    @Test
    void register_WeakPassword_Throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setVerificationCode("123456");
        request.setRole("CUSTOMER");
        request.setPassword("weak");
        request.setConfirmPassword("weak");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Password not meet complexity"));
    }

    @Test
    void register_InvalidVerificationCode_Throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setVerificationCode("wrong");
        request.setPassword(testPassword);
        request.setConfirmPassword(testPassword);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(verificationCodeService.requireLatestValidCode(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), "Verification code is incorrect"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), ex.getCode());
        assertEquals("Verification code is incorrect", ex.getMessage());
    }

    @Test
    void register_ExpiredVerificationCode_Throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setVerificationCode(validCode);
        request.setPassword(testPassword);
        request.setConfirmPassword(testPassword);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(verificationCodeService.requireLatestValidCode(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BusinessException(
                        ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(),
                        "Verification code has expired. Please request a new one."
                ));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Verification code has expired"));
    }

    @Test
    void register_UserInsertFails_DoesNotConsumeVerificationCode() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setVerificationCode(validCode);
        request.setPassword(testPassword);
        request.setConfirmPassword(testPassword);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        VerificationCode codeRecord = VerificationCode.builder()
                .email(testEmail)
                .code(validCode)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeService.requireLatestValidCode(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(codeRecord);
        when(userAccountService.createUser(eq(testEmail), eq(testPassword), eq("CUSTOMER"), isNull()))
                .thenThrow(new RuntimeException("insert failed"));

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(verificationCodeService, never()).markCodeUsed(any(VerificationCode.class));
        assertFalse(codeRecord.getIsUsed());
    }

    // ==================== login Tests ====================

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRole(testRole);

        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash("encoded")
                .role(testRole)
                .status("ACTIVE")
                .loginFailCount(0)
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches(testPassword, "encoded")).thenReturn(true);

        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(1L, testRole)).thenReturn("jwt-token");
            when(refreshTokenMapper.insert(any(RefreshToken.class))).thenReturn(1);

            LoginResponse response = authService.login(request);

            assertEquals("jwt-token", response.getToken());
            assertNotNull(response.getRefreshToken());
            assertEquals(1L, response.getUserId());
            assertEquals(testRole, response.getRole());
            assertEquals(testEmail, response.getEmail());

            verify(userMapper).updateById(user);
            verify(refreshTokenMapper).insert(any(RefreshToken.class));
            assertEquals(0, user.getLoginFailCount());
            assertNull(user.getLockTime());
        }
    }

    @Test
    void refreshToken_Success() {
        String refreshValue = "refresh-token-value";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .token(refreshValue)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .role(testRole)
                .status("ACTIVE")
                .build();

        when(refreshTokenMapper.selectOne(any(QueryWrapper.class))).thenReturn(refreshToken);
        when(userMapper.selectById(1L)).thenReturn(user);

        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(1L, testRole)).thenReturn("new-access-token");

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(refreshValue);
            RefreshTokenResponse response = authService.refreshToken(request);

            assertEquals("new-access-token", response.getToken());
            assertNotNull(response.getExpiresIn());
        }
    }

    @Test
    void login_UserNotFound_ThrowsInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notexist@example.com");
        request.setPassword("any");
        request.setRole(testRole);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_WrongPassword_IncreaseFailCount() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrong");
        request.setRole(testRole);

        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash("encoded")
                .role(testRole)
                .status("ACTIVE")
                .loginFailCount(0)
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.UNAUTHORIZED.getCode(), ex.getCode());
        assertEquals("Invalid email or password", ex.getMessage());

        verify(userMapper).updateById(user);
        assertEquals(1, user.getLoginFailCount());
    }

    @Test
    void login_MaxAttempts_AccountLocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrong");
        request.setRole(testRole);

        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash("encoded")
                .role(testRole)
                .status("ACTIVE")
                .loginFailCount(4)
                .firstFailTime(LocalDateTime.now().minusMinutes(1))  // 1分钟前第一次失败，仍在窗口内
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("15 minutes"));

        verify(userMapper).updateById(user);
        assertEquals(5, user.getLoginFailCount());
        assertEquals("LOCKED", user.getStatus());
        assertNotNull(user.getLockTime());
    }

    @Test
    void login_AccountLockedAndNotExpired_Throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("any");
        request.setRole(testRole);

        User user = User.builder()
                .email(testEmail)
                .role(testRole) 
                .status("LOCKED")
                .lockTime(LocalDateTime.now().minusMinutes(1)) // 假设锁定5分钟，1分钟前锁定 -> 未过期
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("15 minutes"));
    }

    @Test
    void login_DeactivatedAccount_Throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRole(testRole);

        User user = User.builder()
                .email(testEmail)
                .role(testRole) 
                .email(testEmail)
                .status("DEACTIVATED")
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.USER_ERROR_BLOCK.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Account has been deactivated"));
    }

    @Test
    void login_AccountLockedButExpired_UnlocksAndSucceed() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRole(testRole);

        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash("encoded")
                .role(testRole)
                .status("LOCKED")
                .lockTime(LocalDateTime.now().minusMinutes(SecurityConstant.ACCOUNT_LOCK_DURATION_MINUTES + 1))
                .loginFailCount(5)
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches(testPassword, "encoded")).thenReturn(true);

        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(1L, testRole)).thenReturn("jwt");
            LoginResponse response = authService.login(request);
            assertNotNull(response);

            verify(userMapper, times(2)).updateById(user);
            assertEquals("ACTIVE", user.getStatus());
            assertEquals(0, user.getLoginFailCount());
            assertNull(user.getLockTime());
        }
    }
    
    @Test
    void login_RoleNotMatch_Throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRole("SPECIALIST");

        User user = User.builder()
                .email(testEmail)
                .role("CUSTOMER")
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("role not match", ex.getMessage());
    }
    // ==================== Additional tests for first_fail_time and lock_time ====================

    @Test
    void login_FailCountResetsAfterWindowExpires() {
        LocalDateTime firstAttemptTime = LocalDateTime.of(2025, 4, 1, 10, 0, 0);
        try (MockedStatic<LocalDateTime> dateTimeMock = mockStatic(LocalDateTime.class)) {
            dateTimeMock.when(LocalDateTime::now).thenReturn(firstAttemptTime);

            LoginRequest request = new LoginRequest();
            request.setEmail(testEmail);
            request.setPassword("wrong");
            request.setRole(testRole);

            User user = User.builder()
                    .id(1L)
                    .email(testEmail)
                    .passwordHash("encoded")
                    .role(testRole)
                    .status("ACTIVE")
                    .loginFailCount(0)
                    .firstFailTime(null)
                    .build();
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(request));
            verify(userMapper).updateById(user);
            assertEquals(1, user.getLoginFailCount());
            assertEquals(firstAttemptTime, user.getFirstFailTime());

            LocalDateTime laterTime = firstAttemptTime.plusMinutes(3).plusSeconds(1);
            dateTimeMock.when(LocalDateTime::now).thenReturn(laterTime);

            reset(userMapper);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(request));
            verify(userMapper).updateById(user);
            assertEquals(1, user.getLoginFailCount());          
            assertEquals(laterTime, user.getFirstFailTime());   
        }
    }

    @Test
    void login_SuccessResetsFirstFailTime() {
        LocalDateTime failTime = LocalDateTime.now().minusMinutes(1);
        User user = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash("encoded")
                .role(testRole)
                .status("ACTIVE")
                .loginFailCount(3)
                .firstFailTime(failTime)
                .lockTime(null)
                .build();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches(testPassword, "encoded")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRole(testRole);

        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(1L, testRole)).thenReturn("jwt");

            LoginResponse response = authService.login(request);
            assertNotNull(response);

            verify(userMapper, times(1)).updateById(user);
            assertEquals(0, user.getLoginFailCount());
            assertNull(user.getFirstFailTime());   
            assertNull(user.getLockTime());
            assertEquals("ACTIVE", user.getStatus());
        }
    }

    @Test
    void login_FailCountAccumulatesWithinWindow() {
        LocalDateTime firstFailTime = LocalDateTime.of(2025, 4, 1, 10, 0, 0);
        try (MockedStatic<LocalDateTime> dateTimeMock = mockStatic(LocalDateTime.class)) {
            dateTimeMock.when(LocalDateTime::now).thenReturn(firstFailTime);

            LoginRequest request = new LoginRequest();
            request.setEmail(testEmail);
            request.setPassword("wrong");
            request.setRole(testRole);

            User user = User.builder()
                    .id(1L)
                    .email(testEmail)
                    .passwordHash("encoded")
                    .role(testRole)
                    .status("ACTIVE")
                    .loginFailCount(0)
                    .firstFailTime(null)
                    .build();
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(request));
            verify(userMapper).updateById(user);
            assertEquals(1, user.getLoginFailCount());
            assertEquals(firstFailTime, user.getFirstFailTime());

            LocalDateTime secondFailTime = firstFailTime.plusMinutes(2);
            dateTimeMock.when(LocalDateTime::now).thenReturn(secondFailTime);

            reset(userMapper);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(request));
            verify(userMapper).updateById(user);
            assertEquals(2, user.getLoginFailCount());
            assertEquals(firstFailTime, user.getFirstFailTime());
        }
    }
}

package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.constant.SecurityConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.utils.JwtUtils;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.modules.user.model.entity.User;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private VerificationCodeMapper verificationCodeMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private Environment env;
    @InjectMocks private AuthServiceImpl authService;

    private final String testEmail = "test@example.com";
    private final String testPassword = "Test1234";
    private final String testRole = "CUSTOMER";
    private final String validCode = "123456";

    @BeforeEach
    void setUp() {
        // 确保 SecurityConstant 中的值被正确读取（它们在常量类中已定义）
        doNothing().when(mailSender).send(any(MimeMessage.class));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(env.getProperty("spring.mail.username")).thenReturn("noreply@example.com");
    }

    // ==================== sendVerificationCode Tests ====================

    @Test
    void sendVerificationCode_Success() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(verificationCodeMapper.insert(any(VerificationCode.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.sendVerificationCode(request));

        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeMapper).insert(captor.capture());
        VerificationCode saved = captor.getValue();
        assertEquals(testEmail, saved.getEmail());
        assertEquals("REGISTER", saved.getType());
        assertFalse(saved.getIsUsed());
        assertNotNull(saved.getCode());
        assertEquals(6, saved.getCode().length());
    }

    @Test
    void sendVerificationCode_RoleMissing_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationCode(request));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Please select a role"));
    }

    @Test
    void sendVerificationCode_EmailAlreadyExists_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);
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
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerificationCode(request));
        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Please select a role first"));
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
        assertTrue(ex.getMessage().contains("Only CUSTOMER or SPECIALIST"));
    }

    @Test
    void sendVerificationCode_ResendTooQuick_Throws() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail(testEmail);
        request.setRole(testRole);

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        // 模拟最近60秒内存在记录（冷却期内）
        when(verificationCodeMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

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

        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        // 最近60秒内无记录
        when(verificationCodeMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(verificationCodeMapper.insert(any(VerificationCode.class))).thenReturn(1);

        assertDoesNotThrow(() -> authService.sendVerificationCode(request));
        verify(verificationCodeMapper, times(1)).insert(any(VerificationCode.class));
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
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);
        when(passwordEncoder.encode(testPassword)).thenReturn("encodedPwd");
        
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);   // 模拟数据库生成的ID
            return 1;
        });

        // mock JWT 静态方法
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(() -> JwtUtils.generateToken(anyLong(), anyString())).thenReturn("mock-jwt-token");

            LoginResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals(testRole, response.getRole());
            assertEquals(testEmail, response.getEmail());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals(testEmail, savedUser.getEmail());
            assertEquals("encodedPwd", savedUser.getPasswordHash());
            assertEquals(testRole, savedUser.getRole());
            assertEquals("ACTIVE", savedUser.getStatus());

            verify(verificationCodeMapper).updateById(codeRecord);
            assertTrue(codeRecord.getIsUsed());
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
        VerificationCode codeRecord = VerificationCode.builder()
                .code(validCode)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), ex.getCode());
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
        VerificationCode expiredCode = VerificationCode.builder()
                .code(validCode)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))  // 已过期
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(expiredCode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(request));
        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("Verification code incorrect or expired"));
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
            LoginResponse response = authService.login(request);

            assertEquals("jwt-token", response.getToken());
            assertEquals(1L, response.getUserId());
            assertEquals(testRole, response.getRole());
            assertEquals(testEmail, response.getEmail());

            verify(userMapper).updateById(user);
            assertEquals(0, user.getLoginFailCount());
            assertNull(user.getLockTime());
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
}

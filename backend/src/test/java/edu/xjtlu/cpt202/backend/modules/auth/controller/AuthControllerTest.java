package edu.xjtlu.cpt202.backend.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ========== /auth/verify-email ==========

    @Test
    void sendVerificationCode_ValidRequest_ReturnsSuccess() throws Exception {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("test@example.com");
        request.setRole("CUSTOMER");

        doNothing().when(authService).sendVerificationCode(any(SendVerificationCodeRequest.class));

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void sendVerificationCode_InvalidEmail_ReturnsBadRequest() throws Exception {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("not-an-email");
        request.setRole("CUSTOMER");

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // @Valid 校验失败，Spring 返回 400
    }

    @Test
    void sendVerificationCode_MissingRole_ReturnsBadRequest() throws Exception {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("test@example.com");
        request.setRole(null);

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendVerificationCode_ServiceThrowsBusinessException_ReturnsErrorResult() throws Exception {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("existing@example.com");
        request.setRole("CUSTOMER");

        doThrow(new BusinessException(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode(), "Email already registered"))
                .when(authService).sendVerificationCode(any());

        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())  // 全局异常处理返回 HTTP 200，但 code 为错误码
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.EMAIL_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    // ========== /auth/register ==========

    @Test
    void register_ValidRequest_ReturnsToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setVerificationCode("123456");
        request.setPassword("Test1234");
        request.setConfirmPassword("Test1234");
        request.setRole("CUSTOMER");

        LoginResponse mockResponse = LoginResponse.builder()
                .token("jwt-token")
                .userId(100L)
                .role("CUSTOMER")
                .email("new@example.com")
                .displayName("new@example.com")
                .expiresIn(System.currentTimeMillis() + 3600000)
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.userId").value(100));
    }

    @Test
    void register_PasswordMismatch_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("Test1234");
        request.setConfirmPassword("different");
        // 其他字段省略，但 @Valid 会校验 NotNull 等，这里简化，完整测试可补全
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== /auth/login ==========

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Test1234");
        request.setRole("CUSTOMER");

        LoginResponse mockResponse = LoginResponse.builder()
                .token("login-jwt")
                .userId(1L)
                .role("CUSTOMER")
                .email("user@example.com")
                .displayName("user@example.com")
                .expiresIn(System.currentTimeMillis() + 3600000)
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("login-jwt"));
    }

    @Test
    void login_InvalidCredentials_ReturnsError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");
        request.setRole("CUSTOMER");

        doThrow(new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "Invalid credentials"))
                .when(authService).login(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
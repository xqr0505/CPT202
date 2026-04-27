package edu.xjtlu.cpt202.backend.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void login_delegatesToServiceAndReturnsToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .userId(7L)
                .role("CUSTOMER")
                .email("alice@example.com")
                .displayName("Alice")
                .expiresIn(3600L)
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "password": "OldPass123",
                                  "role": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.userId").value(7));

        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(requestCaptor.capture());
        assertEquals("alice@example.com", requestCaptor.getValue().getEmail());
        assertEquals("CUSTOMER", requestCaptor.getValue().getRole());
    }

    @Test
    void login_whenRequestInvalid_returnsValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": "",
                                  "role": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email is required")));

        verifyNoInteractions(authService);
    }

    @Test
    void register_delegatesToServiceAndReturnsLoginResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(LoginResponse.builder()
                .token("registered-token")
                .userId(8L)
                .role("CUSTOMER")
                .build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "verificationCode": "123456",
                                  "password": "NewPass456",
                                  "confirmPassword": "NewPass456",
                                  "role": "CUSTOMER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("registered-token"))
                .andExpect(jsonPath("$.data.userId").value(8));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void sendVerificationCode_validatesAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "role": "CUSTOMER",
                                  "type": "REGISTER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).sendVerificationCode(any(SendVerificationCodeRequest.class));
    }
}

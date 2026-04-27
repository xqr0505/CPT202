package edu.xjtlu.cpt202.backend.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.enums.AccountStatusEnum;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.ChangePasswordDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.dto.UpdateUserProfileDTO;
import edu.xjtlu.cpt202.backend.modules.user.model.vo.UserProfileVO;
import edu.xjtlu.cpt202.backend.modules.user.service.UserAccountService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccountControllerTest {

    private MockMvc mockMvc;
    private UserAccountService userAccountService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        userAccountService = Mockito.mock(UserAccountService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserAccountController(userAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getProfile_delegatesToServiceAndReturnsSuccess() throws Exception {
        UserProfileVO profile = new UserProfileVO();
        profile.setId(7L);
        profile.setFullName("Alice Johnson");
        profile.setEmail("alice@example.com");
        profile.setStatus(AccountStatusEnum.ACTIVE.name());
        when(userAccountService.getCurrentUserProfile()).thenReturn(profile);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));

        verify(userAccountService).getCurrentUserProfile();
    }

    @Test
    void updateProfile_validatesRequestAndDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alice Smith",
                                  "email": "alice@example.com",
                                  "phoneNumber": "+86 13900139000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<UpdateUserProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateUserProfileDTO.class);
        verify(userAccountService).updateCurrentUserProfile(requestCaptor.capture());
        assertEquals("Alice Smith", requestCaptor.getValue().getFullName());
    }

    @Test
    void updateProfile_whenEmailInvalid_returnsValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Please enter a valid email address."));

        verifyNoInteractions(userAccountService);
    }

    @Test
    void changePassword_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/user/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass123",
                                  "newPassword": "NewPass456",
                                  "confirmationPassword": "NewPass456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userAccountService).changePassword(any(ChangePasswordDTO.class));
    }

    @Test
    void deactivate_delegatesCurrentPasswordToService() throws Exception {
        mockMvc.perform(post("/api/user/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userAccountService).deactivateCurrentUserAccount("OldPass123");
    }
}

package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.modules.ai.service.Assistant;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ai.openai.api-key=test-openai-key",
        "ai.openai.model-name=gpt-4o-mini",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Assistant assistant;

    @MockBean
    private UserMapper userMapper;

    @Test
    void chatShouldReturnSuccessForCustomer() throws Exception {
        when(assistant.chat(anyString())).thenReturn("Hello from AI");

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(customerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Hello from AI"));
    }

    @Test
    void chatShouldReturnValidationErrorWhenMessageBlank() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(customerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void chatShouldReturnUnauthorizedWhenMissingAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void chatShouldReturnForbiddenForSpecialist() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(specialistAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                1001L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")
        );
    }

    private Authentication specialistAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                2001L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_SPECIALIST")
        );
    }
}

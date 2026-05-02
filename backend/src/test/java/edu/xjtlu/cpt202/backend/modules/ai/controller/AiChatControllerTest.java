package edu.xjtlu.cpt202.backend.modules.ai.controller;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.config.SecurityConfig;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.modules.ai.profiling.AiChatProfiler;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiChatService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author QiranXiao
 * @since 2026/4/15
 *
 */
@SpringBootTest(classes = AiChatControllerTest.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private UserMapper userMapper;

    @Test
    void chatShouldStreamSseEventsForAuthenticatedUser() throws Exception {
        when(aiChatService.streamChat(anyString())).thenReturn(
                new TestTokenStream(List.of("Hello ", "from AI"))
        );

        MvcResult result = mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(customerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Hello"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andExpect(content().string(containsString("event:chat")))
                .andExpect(content().string(containsString("\"content\":\"Hello \"")))
                .andExpect(content().string(containsString("\"content\":\"from AI\"")))
                .andExpect(content().string(containsString("event:done")));
    }

    @Test
    void chatShouldStreamErrorPayloadWhenTokenStreamFails() throws Exception {
        when(aiChatService.streamChat(anyString())).thenReturn(
                new FailingTokenStream(new BusinessException(400, "No bookings matched"))
        );

        MvcResult result = mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(customerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Show my bookings"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andExpect(content().string(allOf(
                        containsString("event:done"),
                        containsString("\"code\":400"),
                        containsString("\"message\":\"No bookings matched\"")
                )));
    }

    @Test
    void chatSyncShouldReturnValidationErrorWhenMessageBlank() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat/sync")
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
    void clearMemoryShouldReturnSuccessForCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/chat/memory")
                        .with(authentication(customerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void clearMemoryShouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/chat/memory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void clearMemoryShouldReturnForbiddenForSpecialist() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/chat/memory")
                        .with(authentication(specialistAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
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

    @Test
    void chatShouldReturnForbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(authentication(adminAuthentication()))
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

    private Authentication adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                3001L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );
    }

    private static class TestTokenStream implements TokenStream {

        private final List<String> chunks;
        private Consumer<String> onNext = ignored -> {
        };
        private Consumer<Response<AiMessage>> onComplete = ignored -> {
        };
        private Consumer<Throwable> onError = ignored -> {
        };

        private TestTokenStream(List<String> chunks) {
            this.chunks = chunks;
        }

        @Override
        public TokenStream onNext(Consumer<String> onNext) {
            this.onNext = onNext;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted) {
            return this;
        }

        @Override
        public TokenStream onComplete(Consumer<Response<AiMessage>> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            try {
                for (String chunk : chunks) {
                    onNext.accept(chunk);
                }
                onComplete.accept(Response.from(AiMessage.aiMessage(String.join("", chunks))));
            } catch (RuntimeException exception) {
                onError.accept(exception);
            }
        }
    }

    private static class FailingTokenStream implements TokenStream {

        private final RuntimeException failure;
        private Consumer<Throwable> onError = ignored -> {
        };

        private FailingTokenStream(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public TokenStream onNext(Consumer<String> onNext) {
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> onRetrieved) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> onToolExecuted) {
            return this;
        }

        @Override
        public TokenStream onComplete(Consumer<Response<AiMessage>> onComplete) {
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            onError.accept(failure);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            MybatisPlusAutoConfiguration.class
    })
    @Import({AiChatController.class, SecurityConfig.class, GlobalExceptionHandler.class, AiChatProfiler.class, CommonProperties.class})
    static class TestApplication {
    }
}

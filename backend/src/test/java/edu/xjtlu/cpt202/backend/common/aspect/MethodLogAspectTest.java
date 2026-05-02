package edu.xjtlu.cpt202.backend.common.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.xjtlu.cpt202.backend.common.properties.CommonProperties;
import edu.xjtlu.cpt202.backend.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MethodLogAspectTest {

    private AnnotationConfigApplicationContext applicationContext;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        applicationContext = new AnnotationConfigApplicationContext(TestConfig.class);
        logger = (Logger) LoggerFactory.getLogger(MethodLogAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        if (logger != null && listAppender != null) {
            logger.detachAppender(listAppender);
        }
        RequestContextHolder.resetRequestAttributes();
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    void shouldLogControllerAndServiceInvocation() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/logging");
        when(request.getMethod()).thenReturn("GET");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        TestController controller = applicationContext.getBean(TestController.class);
        controller.getValue("hello");

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("layer=controller")
                        && event.getFormattedMessage().contains("status=success")));
        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("layer=service")
                        && event.getFormattedMessage().contains("status=success")));
    }

    @Test
    void shouldLogSlowCallAsWarnAndSanitizeSpecialArguments() {
        TestService service = applicationContext.getBean(TestService.class);
        service.slowMethod("x".repeat(200), new SseEmitter());

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("method=TestService#slowMethod")
                        && event.getFormattedMessage().contains("...(104)")));
    }

    @Test
    void shouldLogExceptionAsError() {
        TestService service = applicationContext.getBean(TestService.class);
        try {
            service.fail();
        } catch (IllegalStateException ignored) {
        }

        assertTrue(listAppender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("status=error")
                        && event.getFormattedMessage().contains("IllegalStateException")));
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        CommonProperties commonProperties() {
            CommonProperties properties = new CommonProperties();
            properties.getLogging().setEnabled(true);
            properties.getLogging().setArgMaxLength(64);
            properties.getLogging().getSlowThreshold().setControllerMs(1);
            properties.getLogging().getSlowThreshold().setServiceMs(1);
            return properties;
        }

        @Bean
        MethodLogAspect methodLogAspect(CommonProperties commonProperties) {
            return new MethodLogAspect(commonProperties);
        }

        @Bean
        TestService testService() {
            return new TestService();
        }

        @Bean
        TestController testController(TestService testService) {
            return new TestController(testService);
        }
    }

    @Service
    static class TestService {

        public String work(String input) {
            return "ok:" + input;
        }

        public String slowMethod(String value, SseEmitter emitter) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return value + emitter;
        }

        public String fail() {
            throw new IllegalStateException("boom");
        }
    }

    @RestController
    static class TestController {

        private final TestService testService;

        TestController(TestService testService) {
            this.testService = testService;
        }

        @GetMapping("/test/logging")
        public Result<String> getValue(String input) {
            return Result.success(testService.work(input));
        }
    }
}

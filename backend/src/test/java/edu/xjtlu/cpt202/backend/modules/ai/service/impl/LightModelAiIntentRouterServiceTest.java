package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.modules.ai.config.AiIntentRouterProperties;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @since 2026/5/5
 */
class LightModelAiIntentRouterServiceTest {

    @Test
    void shouldRouteExplicitBookingOrderToBookingIntent() {
        ChatLanguageModel lightModel = mock(ChatLanguageModel.class);
        LightModelAiIntentRouterService service = new LightModelAiIntentRouterService(
                lightModel,
                properties()
        );

        AiIntent intent = service.resolveIntent(1001L, "please place booking order now");

        assertThat(intent).isEqualTo(AiIntent.BOOKING);
        verifyNoInteractions(lightModel);
    }

    @Test
    void shouldNotRouteGenericAvailabilityQueryToBookingIntent() {
        ChatLanguageModel lightModel = mock(ChatLanguageModel.class);
        when(lightModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("KNOWLEDGE")));
        LightModelAiIntentRouterService service = new LightModelAiIntentRouterService(
                lightModel,
                properties()
        );

        AiIntent intent = service.resolveIntent(1001L, "show me available specialists tomorrow at 10");

        assertThat(intent).isEqualTo(AiIntent.KNOWLEDGE);
        verify(lightModel).generate(anyList());
    }

    @Test
    void shouldFallbackToKnowledgeAndCancelTaskWhenModelTimesOut() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1);
        ChatLanguageModel lightModel = messages -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException exception) {
                interrupted.countDown();
                throw new RuntimeException("interrupted", exception);
            }
            return Response.from(AiMessage.from("BOOKING"));
        };
        RecordingExecutorService executorService = new RecordingExecutorService();
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        properties.setTimeoutMs(50L);
        LightModelAiIntentRouterService service = new LightModelAiIntentRouterService(
                lightModel,
                properties,
                executorService
        );

        AiIntent intent = service.resolveIntent(1001L, "show me specialists around friday evening");

        assertThat(intent).isEqualTo(AiIntent.KNOWLEDGE);
        assertThat(executorService.lastTask).isNotNull();
        assertThat(executorService.lastTask.isCancelled()).isTrue();
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    private static AiIntentRouterProperties properties() {
        AiIntentRouterProperties properties = new AiIntentRouterProperties();
        properties.setTimeoutMs(800L);
        return properties;
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {

        private volatile FutureTask<?> lastTask;

        @Override
        public void shutdown() {
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            Thread thread = new Thread(command, "router-timeout-test");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(java.util.concurrent.Callable<T> callable) {
            FutureTask<T> task = new FutureTask<>(callable);
            lastTask = task;
            return task;
        }
    }
}

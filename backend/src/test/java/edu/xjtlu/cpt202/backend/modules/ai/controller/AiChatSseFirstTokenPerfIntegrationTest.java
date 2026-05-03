package edu.xjtlu.cpt202.backend.modules.ai.controller;

import edu.xjtlu.cpt202.backend.BackendApplication;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import edu.xjtlu.cpt202.backend.testsupport.DotenvTestSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * @author QiranXiao
 * @since 2026/5/2
 *
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Disabled("Real AI perf test; requires model/tool credentials and local infra, run manually.")
class AiChatSseFirstTokenPerfIntegrationTest {

    private static final int DEFAULT_CONCURRENCY = 5;
    private static final int DEFAULT_REPEAT = 1;
    private static final List<String> QUESTIONS = List.of(
        "What is the refund policy for cancelling within 2 hours of the appointment?",
        "How can I sort the specialists by fee from low to high?",
        "What does 'Pending' status mean in my bookings?",
        "Can I reschedule my appointment to a different specialist?",
        "My search results are empty. How can I fix this quickly?",
        "Why is the 'Cancel' button greyed out on my booking?",
        "I clicked 'Confirm' but got a 'Time slot already booked' error. Why?",
        "I submitted my notes, but it says 'unsupported characters'. What should I do?",
        "If I book a specialist but need to change the time later, will it be Confirmed automatically?",
        "I need the cheapest specialist available this coming Saturday. What is the best search order?"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @DynamicPropertySource
    static void loadDotenv(DynamicPropertyRegistry registry) {
        var dotenv = DotenvTestSupport.loadRepoRootDotenv();

        registry.add("spring.mvc.async.request-timeout", () -> "60000");
        registry.add("ai.openai.api-key", () -> firstNonBlankOrNull(System.getenv("OPENAI_API_KEY"), dotenv.get("OPENAI_API_KEY")));
        registry.add("ai.openai.model-name", () -> firstNonBlankOrNull(System.getenv("OPENAI_MODEL_NAME"), dotenv.get("OPENAI_MODEL_NAME")));
        registry.add("ai.openai.base-url", () -> firstNonBlankOrNull(System.getenv("OPENAI_BASE_URL"), dotenv.get("OPENAI_BASE_URL")));

        registry.add("DASHSCOPE_API_KEY", () -> firstNonBlankOrNull(System.getenv("DASHSCOPE_API_KEY"), dotenv.get("DASHSCOPE_API_KEY")));


        String dbHost = resolveHost(
                System.getenv("DB_HOST"),
                dotenv.get("DB_HOST"),
                "127.0.0.1",
                List.of(9001, 3306)
        );
        String dbPort = resolvePort(
                System.getenv("DB_PORT"),
                dotenv.get("DB_PORT"),
                dbHost,
                List.of(9001, 3306),
                "3306"
        );
        String dbName = firstNonBlank(System.getenv("DB_NAME"), dotenv.get("DB_NAME"), "cpt202_consultancy");
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%%2B8"
                        .formatted(dbHost, dbPort, dbName));

        registry.add("spring.datasource.username", () -> firstNonBlank(System.getenv("DB_USERNAME"), dotenv.get("DB_USERNAME"), "root"));
        registry.add("spring.datasource.password", () -> firstNonBlank(System.getenv("DB_PASSWORD"), dotenv.get("DB_PASSWORD"), "Root@123"));

        String redisHost = resolveHost(
                System.getenv("REDIS_HOST"),
                dotenv.get("REDIS_HOST"),
                "127.0.0.1",
                List.of(9002, 6379)
        );
        String redisPort = resolvePort(
                System.getenv("REDIS_PORT"),
                dotenv.get("REDIS_PORT"),
                redisHost,
                List.of(9002, 6379),
                "9002"
        );
        registry.add("spring.data.redis.host", () -> redisHost);
        registry.add("spring.data.redis.port", () -> redisPort);

        String ragRedisHost = resolveHost(
                System.getenv("AI_RAG_REDIS_HOST"),
                dotenv.get("AI_RAG_REDIS_HOST"),
                redisHost,
                List.of(parseIntOrDefault(redisPort, 9002), 9002, 6379)
        );
        String ragRedisPort = resolvePort(
                System.getenv("AI_RAG_REDIS_PORT"),
                dotenv.get("AI_RAG_REDIS_PORT"),
                ragRedisHost,
                List.of(parseIntOrDefault(redisPort, 9002), 9002, 6379),
                redisPort
        );
        registry.add("ai.rag.redis.host", () -> ragRedisHost);
        registry.add("ai.rag.redis.port", () -> ragRedisPort);
    }

    @Test
    void shouldMeasureTimeToFirstTokenAcrossQuestionsConcurrently() throws Exception {
        requireExternalDependencies();

        int concurrency = Integer.getInteger("ai.chat.perf.concurrency", DEFAULT_CONCURRENCY);
        int repeat = Integer.getInteger("ai.chat.perf.repeat", DEFAULT_REPEAT);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        try {
            long suiteStartNs = System.nanoTime();
            List<CompletableFuture<RunResult>> futures = new ArrayList<>();
            for (int round = 1; round <= repeat; round++) {
                int currentRound = round;
                for (String question : QUESTIONS) {
                    futures.add(CompletableFuture.supplyAsync(
                            () -> runSingleQuestion(question, currentRound),
                            executor
                    ));
                }
            }

            List<RunResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            long suiteElapsedMs = Duration.ofNanos(System.nanoTime() - suiteStartNs).toMillis();

            printPerRequestResults(results);
            printSummary(results, concurrency, repeat);
            printThroughput(results, suiteElapsedMs, concurrency, repeat);

            long successCount = results.stream().filter(RunResult::success).count();
            if (successCount == 0) {

                System.out.println("[ai-chat-ttft] no successful samples (successCount=0)");
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    private void printThroughput(List<RunResult> results, long suiteElapsedMs, int concurrency, int repeat) {
        long total = results.size();
        long success = results.stream().filter(RunResult::success).count();
        long fail = total - success;

        if (suiteElapsedMs <= 0) {
            System.out.printf(
                    "[ai-chat-throughput] elapsedMs=%d total=%d success=%d fail=%d concurrency=%d repeat=%d%n",
                    suiteElapsedMs,
                    total,
                    success,
                    fail,
                    concurrency,
                    repeat
            );
            return;
        }

        double elapsedSeconds = suiteElapsedMs / 1000.0;
        double rpsTotal = total / elapsedSeconds;
        double rpsSuccess = success / elapsedSeconds;

        System.out.printf(
                "[ai-chat-throughput] elapsedMs=%d total=%d success=%d fail=%d rpsTotal=%.2f rpsSuccess=%.2f concurrency=%d repeat=%d%n",
                suiteElapsedMs,
                total,
                success,
                fail,
                rpsTotal,
                rpsSuccess,
                concurrency,
                repeat
        );
    }

    private RunResult runSingleQuestion(String question, int round) {
        long startNs = System.nanoTime();
        try {
            MvcResult startResult = mockMvc.perform(post("/api/v1/ai/chat")
                            .with(authentication(customerAuthentication()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "message": "%s"
                                    }
                                    """.formatted(escapeJson(question))))
                    .andExpect(request().asyncStarted())
                    .andReturn();


            startResult.getAsyncResult(60_000);

            String payload = mockMvc.perform(asyncDispatch(startResult))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            long ttftMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();
            SseParseResult sse = parseSse(payload);
            boolean success = sse.hasChatEvent();
            String reason = success ? "ok" : sse.reason();
            String debug = success ? "" : shrink(payload, 600);
            return new RunResult(question, round, success, ttftMs, reason, debug);
        } catch (Exception exception) {
            long ttftMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();
            String msg = exception.getMessage();
            String reason = exception.getClass().getSimpleName() + (msg == null || msg.isBlank() ? "" : (": " + msg));
            return new RunResult(question, round, false, ttftMs, reason, "");
        }
    }

    private void printPerRequestResults(List<RunResult> results) {
        results.stream()
                .sorted(Comparator.comparingInt(RunResult::round).thenComparing(RunResult::question))
                .forEach(result -> {
                    System.out.printf(
                            "[ai-chat-ttft] round=%d success=%s ttftMs=%d question=\"%s\" reason=%s%n",
                            result.round(),
                            result.success(),
                            result.ttftMs(),
                            result.question(),
                            result.reason()
                    );
                    if (!result.success() && result.debugSnippet() != null && !result.debugSnippet().isBlank()) {
                        System.out.printf("[ai-chat-ttft-debug] %s%n", result.debugSnippet());
                    }
                });
    }

    private void printSummary(List<RunResult> results, int concurrency, int repeat) {
        List<Long> successValues = results.stream()
                .filter(RunResult::success)
                .map(RunResult::ttftMs)
                .sorted()
                .toList();

        long successCount = successValues.size();
        long failCount = results.size() - successCount;
        System.out.printf(
                "[ai-chat-ttft-summary] total=%d success=%d fail=%d concurrency=%d repeat=%d%n",
                results.size(),
                successCount,
                failCount,
                concurrency,
                repeat
        );

        if (successValues.isEmpty()) {
            System.out.println("[ai-chat-ttft-summary] no successful samples");
            return;
        }

        long min = successValues.get(0);
        long max = successValues.get(successValues.size() - 1);
        double avg = successValues.stream().mapToLong(Long::longValue).average().orElse(0D);
        long p50 = percentile(successValues, 0.50);
        long p95 = percentile(successValues, 0.95);
        System.out.printf(
                "[ai-chat-ttft-summary] min=%d avg=%.2f p50=%d p95=%d max=%d%n",
                min,
                avg,
                p50,
                p95,
                max
        );
    }

    private void requireExternalDependencies() {
        //support both:
        // 1) process env vars, and 2) repo-root .env (loaded via DotenvTestSupport).
        var dotenv = DotenvTestSupport.loadRepoRootDotenv();
        Assumptions.assumeTrue(
                firstNonBlankOrNull(System.getenv("OPENAI_API_KEY"), dotenv.get("OPENAI_API_KEY")) != null,
                "OPENAI_API_KEY is required (env var or repo-root .env)"
        );
        Assumptions.assumeTrue(
                firstNonBlankOrNull(System.getenv("OPENAI_MODEL_NAME"), dotenv.get("OPENAI_MODEL_NAME")) != null,
                "OPENAI_MODEL_NAME is required (env var or repo-root .env)"
        );
        Assumptions.assumeTrue(
                firstNonBlankOrNull(System.getenv("DASHSCOPE_API_KEY"), dotenv.get("DASHSCOPE_API_KEY")) != null,
                "DASHSCOPE_API_KEY is required (env var or repo-root .env)"
        );
    }

    private boolean isSet(String key) {
        String value = System.getenv(key);
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private static String firstNonBlankOrNull(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private static String resolvePort(
            String envPort,
            String dotenvPort,
            String host,
            List<Integer> candidates,
            String fallback
    ) {
        if (envPort != null && !envPort.isBlank()) {
            return envPort;
        }
        // Dotenv port is optional: use it only when actually reachable on resolved host.
        if (dotenvPort != null && !dotenvPort.isBlank()) {
            int parsed = parseIntOrDefault(dotenvPort, -1);
            if (parsed > 0 && canConnect(host, parsed, 200)) {
                return dotenvPort;
            }
        }
        for (Integer candidate : candidates) {
            if (candidate != null && canConnect(host, candidate, 200)) {
                return Integer.toString(candidate);
            }
        }
        return fallback;
    }

    private static String resolveHost(String envHost, String dotenvHost, String fallback, List<Integer> probePorts) {
        // 1) Explicit process env has highest priority; if user set it, trust it.
        if (envHost != null && !envHost.isBlank()) {
            return envHost;
        }
        // 2) Repo .env host is convenient, but may be docker-network-only (e.g. "redis").
        if (dotenvHost != null && !dotenvHost.isBlank()) {
            if (canResolve(dotenvHost) && canConnectAny(dotenvHost, probePorts, 200)) {
                return dotenvHost;
            }
        }
        // 3) Fallback to localhost-style host for host-run tests.
        return fallback;
    }

    private static boolean canResolve(String host) {
        try {
            InetAddress.getByName(host);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean canConnectAny(String host, List<Integer> ports, int timeoutMs) {
        for (Integer port : ports) {
            if (port != null && canConnect(host, port, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canConnect(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long percentile(List<Long> sortedValues, double ratio) {
        int index = (int) Math.ceil(sortedValues.size() * ratio) - 1;
        int boundedIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(boundedIndex);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                1001L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")
        );
    }

    private record RunResult(String question, int round, boolean success, long ttftMs, String reason, String debugSnippet) {
    }

    private record SseParseResult(boolean hasChatEvent, boolean hasDoneEvent, boolean hasCodeField, String reason) {
    }

    private SseParseResult parseSse(String payload) {
        if (payload == null || payload.isBlank()) {
            return new SseParseResult(false, false, false, "empty response");
        }
        boolean hasChat = payload.contains("event:chat") || payload.contains("event: chat");
        boolean hasDone = payload.contains("event:done") || payload.contains("event: done");
        boolean hasCode = payload.contains("\"code\":");
        if (hasChat) {
            return new SseParseResult(true, hasDone, hasCode, "ok");
        }
        if (hasDone && hasCode) {
            return new SseParseResult(false, true, true, "done-with-error");
        }
        if (hasDone) {
            return new SseParseResult(false, true, hasCode, "done-without-chat");
        }
        return new SseParseResult(false, false, hasCode, "no sse events");
    }

    private String shrink(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", "");
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...(" + normalized.length() + ")";
    }
}

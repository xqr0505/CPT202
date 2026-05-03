package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.model.output.Response;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.CancelTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.CancelWorkflowService;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dedicated cancellation workflow service.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
@Service
public class CancelWorkflowServiceImpl implements CancelWorkflowService {

    static final String CANCEL_TASK_ABORTED_MARKER = "[CANCEL_TASK_ABORTED]";
    static final String TRIGGER_CANCEL_MODAL_PREFIX = "[TRIGGER_CANCEL_MODAL:";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,18})\\b");
    private static final int STREAM_CHUNK_SIZE = 24;
    private static final long STREAM_CHUNK_DELAY_MS = 24L;
    private static final Pattern CANCEL_INTENT_PATTERN = Pattern.compile(
            "(\\bcancel\\b|\\bcancellation\\b|\\bvoid\\b|\\bcall off\\b|取消|撤销|取消预约|退订)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOOKING_HINT_PATTERN = Pattern.compile(
            "(\\bbooking\\b|\\bappointment\\b|预约|订单|单号|id)",
            Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ABORT_PHRASES = Set.of(
            "forget it",
            "never mind",
            "nevermind",
            "cancel this process",
            "stop",
            "quit"
    );
    private static final Set<String> CANCELLABLE_STATUSES = Set.of("PENDING", "CONFIRMED");

    private final CancelTaskStateStore cancelTaskStateStore;
    private final CancelWorkflowAssistant cancelWorkflowAssistant;
    private final BookingService bookingService;
    private final AiIntentRouterService aiIntentRouterService;

    public CancelWorkflowServiceImpl(
            CancelTaskStateStore cancelTaskStateStore,
            CancelWorkflowAssistant cancelWorkflowAssistant,
            BookingService bookingService,
            AiIntentRouterService aiIntentRouterService
    ) {
        this.cancelTaskStateStore = cancelTaskStateStore;
        this.cancelWorkflowAssistant = cancelWorkflowAssistant;
        this.bookingService = bookingService;
        this.aiIntentRouterService = aiIntentRouterService;
    }

    @Override
    public boolean hasActiveTask(Long userId) {
        return cancelTaskStateStore.get(userId).isPresent();
    }

    @Override
    public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
        if (looksLikeCancelRequest(originalUserMessage)) {
            return true;
        }
        return aiIntentRouterService.resolveIntent(userId, originalUserMessage) == AiIntent.CANCEL;
    }

    @Override
    public String handle(Long userId, String normalizedUserMessage) {
        String originalUserMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
        CancelTaskState state = cancelTaskStateStore.get(userId)
                .orElseGet(() -> createInitialState());
        return handleInternal(userId, normalizedUserMessage, originalUserMessage, state);
    }

    @Override
    public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
        String reply = handle(userId, normalizedUserMessage);
        return new SingleReplyTokenStream(reply);
    }

    private String handleInternal(
            Long userId,
            String normalizedUserMessage,
            String originalUserMessage,
            CancelTaskState state
    ) {
        if (isAbortShortcut(originalUserMessage)) {
            cancelTaskStateStore.clear(userId);
            return CANCEL_TASK_ABORTED_MARKER + " Cancellation flow closed.";
        }

        String abortProbe = cancelWorkflowAssistant.process(
                normalizedUserMessage,
                Optional.ofNullable(state.getTaskStateText()).orElse("identifying booking to cancel")
        );
        if (abortProbe != null && abortProbe.startsWith(CANCEL_TASK_ABORTED_MARKER)) {
            cancelTaskStateStore.clear(userId);
            return abortProbe;
        }

        if (state.getStep() == null) {
            state.setStep(CancelTaskState.Step.IDENTIFY);
        }

        if (state.getStep() == CancelTaskState.Step.IDENTIFY) {
            return handleIdentifyStep(userId, originalUserMessage, state);
        }

        return handleValidateStep(userId, state);
    }

    private String handleIdentifyStep(Long userId, String originalUserMessage, CancelTaskState state) {
        List<BookingItemVO> candidates = loadCancellableBookings(userId);
        if (candidates.isEmpty()) {
            cancelTaskStateStore.clear(userId);
            return "You do not have any cancellable bookings right now.";
        }

        Long identifiedBookingId = identifyBookingId(originalUserMessage, candidates);
        if (identifiedBookingId == null) {
            state.setStep(CancelTaskState.Step.IDENTIFY);
            state.setCandidateBookingIds(candidates.stream()
                    .map(BookingItemVO::getId)
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .toList());
            state.setTaskStateText("waiting for the user to choose one booking to cancel");
            cancelTaskStateStore.save(userId, state);
            return buildCandidatePrompt(candidates);
        }

        state.setTargetBookingId(identifiedBookingId);
        state.setStep(CancelTaskState.Step.VALIDATE);
        state.setCandidateBookingIds(List.of(identifiedBookingId));
        state.setTaskStateText("booking identified, validating cancellation quote");
        cancelTaskStateStore.save(userId, state);
        return handleValidateStep(userId, state);
    }

    private String handleValidateStep(Long userId, CancelTaskState state) {
        Long bookingId = state.getTargetBookingId();
        if (bookingId == null) {
            cancelTaskStateStore.clear(userId);
            return CANCEL_TASK_ABORTED_MARKER + " Cancellation flow lost the target booking. Please start again.";
        }

        BookingCancelQuoteVO quote = bookingService.customerCancellationQuote(bookingId, userId);
        cancelTaskStateStore.clear(userId);

        if (!quote.isAllowed()) {
            return "Sorry, this booking cannot be cancelled. " + safeText(quote.getMessage());
        }

        return """
                Please review the cancellation details and confirm manually.
                %s%d]
                """.formatted(
                TRIGGER_CANCEL_MODAL_PREFIX,
                bookingId
        );
    }

    private CancelTaskState createInitialState() {
        return CancelTaskState.builder()
                .step(CancelTaskState.Step.IDENTIFY)
                .taskStateText("identifying which booking the user wants to cancel")
                .build();
    }

    private boolean isAbortShortcut(String originalUserMessage) {
        String normalized = normalize(originalUserMessage);
        for (String phrase : ABORT_PHRASES) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private List<BookingItemVO> loadCancellableBookings(Long userId) {
        BookingPageQueryDTO queryDTO = new BookingPageQueryDTO();
        queryDTO.setPageNo(1);
        queryDTO.setPageSize(20);
        queryDTO.setTab("UPCOMING");
        PageResult<BookingItemVO> result = bookingService.getBookingList(userId, queryDTO);
        if (result == null || result.getList() == null) {
            return List.of();
        }
        return result.getList().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStatus() != null && CANCELLABLE_STATUSES.contains(item.getStatus().toUpperCase(Locale.ROOT)))
                .toList();
    }

    private Long identifyBookingId(String originalUserMessage, List<BookingItemVO> candidates) {
        Long directId = extractDirectBookingId(originalUserMessage, candidates);
        if (directId != null) {
            return directId;
        }

        String normalizedMessage = normalize(originalUserMessage);
        Map<Long, Integer> scores = new LinkedHashMap<>();
        for (BookingItemVO candidate : candidates) {
            long score = scoreCandidate(normalizedMessage, candidate);
            if (score > 0 && candidate.getId() != null) {
                scores.put(Long.valueOf(candidate.getId()), (int) score);
            }
        }

        if (scores.isEmpty()) {
            return null;
        }

        List<Map.Entry<Long, Integer>> sorted = scores.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .toList();
        if (sorted.size() > 1 && Objects.equals(sorted.get(0).getValue(), sorted.get(1).getValue())) {
            return null;
        }
        return sorted.get(0).getKey();
    }

    private Long extractDirectBookingId(String originalUserMessage, List<BookingItemVO> candidates) {
        Matcher matcher = NUMBER_PATTERN.matcher(Optional.ofNullable(originalUserMessage).orElse(""));
        Set<String> candidateIds = candidates.stream()
                .map(BookingItemVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        while (matcher.find()) {
            String value = matcher.group(1);
            if (candidateIds.contains(value)) {
                return Long.valueOf(value);
            }
        }
        return null;
    }

    private long scoreCandidate(String normalizedMessage, BookingItemVO candidate) {
        long score = 0L;
        if (containsNormalized(normalizedMessage, candidate.getSpecialistName())) {
            score += 4;
        }
        if (containsNormalized(normalizedMessage, candidate.getServiceName())) {
            score += 3;
        }
        if (candidate.getAppointmentDateTime() != null) {
            String dateToken = candidate.getAppointmentDateTime().toLocalDate().toString();
            String timeToken = candidate.getAppointmentDateTime().toLocalTime().withSecond(0).withNano(0).toString();
            if (normalizedMessage.contains(dateToken.toLowerCase(Locale.ROOT))) {
                score += 3;
            }
            if (normalizedMessage.contains(timeToken.toLowerCase(Locale.ROOT))) {
                score += 2;
            }
        }
        return score;
    }

    private boolean containsNormalized(String normalizedMessage, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return normalizedMessage.contains(normalize(value));
    }

    private String buildCandidatePrompt(List<BookingItemVO> candidates) {
        List<String> lines = new ArrayList<>();
        lines.add("I found multiple cancellable bookings. Please reply with the booking ID you want to cancel:");
        lines.add("");
        lines.add("| Booking ID | Specialist | Service | Appointment Time | Status |");
        lines.add("| --- | --- | --- | --- | --- |");
        for (BookingItemVO item : candidates) {
            lines.add("| %s | %s | %s | %s | %s |".formatted(
                    safeText(item.getId()),
                    safeText(item.getSpecialistName()),
                    safeText(item.getServiceName()),
                    item.getAppointmentDateTime() == null ? "unknown time" : item.getAppointmentDateTime().format(DATE_TIME_FORMATTER),
                    safeText(item.getStatus())
            ));
        }
        return String.join("\n", lines);
    }

    private String formatDateTime(BookingCancelQuoteVO quote) {
        if (quote == null || quote.getBookingStartAt() == null) {
            return "unknown";
        }
        return quote.getBookingStartAt().format(DATE_TIME_FORMATTER);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.toPlainString();
    }

    private String normalize(String userMessage) {
        if (userMessage == null) {
            return "";
        }
        return userMessage.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean looksLikeCancelRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.trim();
        if (!CANCEL_INTENT_PATTERN.matcher(normalized).find()) {
            return false;
        }
        return BOOKING_HINT_PATTERN.matcher(normalized).find() || normalized.contains("取消");
    }

    private static class SingleReplyTokenStream implements TokenStream {

        private final String reply;
        private java.util.function.Consumer<String> onNext = ignored -> { };
        private java.util.function.Consumer<Response<dev.langchain4j.data.message.AiMessage>> onComplete = ignored -> { };
        private java.util.function.Consumer<Throwable> onError = ignored -> { };

        private SingleReplyTokenStream(String reply) {
            this.reply = reply;
        }

        @Override
        public TokenStream onRetrieved(java.util.function.Consumer<List<dev.langchain4j.rag.content.Content>> consumer) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution> consumer) {
            return this;
        }

        @Override
        public TokenStream onComplete(java.util.function.Consumer<Response<dev.langchain4j.data.message.AiMessage>> consumer) {
            this.onComplete = consumer;
            return this;
        }

        @Override
        public TokenStream onError(java.util.function.Consumer<Throwable> consumer) {
            this.onError = consumer;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            try {
                emitTextChunks(reply);
                onComplete.accept(Response.from(dev.langchain4j.data.message.AiMessage.from(reply)));
            } catch (Throwable throwable) {
                onError.accept(throwable);
            }
        }

        @Override
        public TokenStream onNext(java.util.function.Consumer<String> consumer) {
            this.onNext = consumer;
            return this;
        }

        private void emitTextChunks(String text) {
            if (text == null || text.isBlank()) {
                return;
            }
            for (int index = 0; index < text.length(); index += STREAM_CHUNK_SIZE) {
                int endIndex = Math.min(index + STREAM_CHUNK_SIZE, text.length());
                onNext.accept(text.substring(index, endIndex));
                if (endIndex < text.length()) {
                    try {
                        Thread.sleep(STREAM_CHUNK_DELAY_MS);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}

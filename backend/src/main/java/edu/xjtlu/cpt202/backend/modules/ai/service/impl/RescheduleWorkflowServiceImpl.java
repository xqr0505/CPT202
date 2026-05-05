package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.RescheduleTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowService;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RescheduleWorkflowServiceImpl implements RescheduleWorkflowService {

    static final String RESCHEDULE_TASK_ABORTED_MARKER = "[RESCHEDULE_TASK_ABORTED]";
    static final String TRIGGER_RESCHEDULE_MODAL_PREFIX = "[TRIGGER_RESCHEDULE_MODAL:";

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESCHEDULE_INTENT_PATTERN = Pattern.compile(
            "(\\breschedul(?:e|ing)?\\b|\\bchange time\\b|\\bmove my booking\\b|\\bmove appointment\\b|\\bchange appointment\\b|\\bchange booking\\b|\\bmove booking\\b|\\breschedul\\b|\\breschedule\\b.*\\bto\\b|\\bmove\\b.*\\bto\\b)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RESCHEDULE_KNOWLEDGE_QUESTION_PATTERN = Pattern.compile(
            "\\b(what|why|how|when|which|who|where|explain|policy|policies|rule|rules|guide|meaning|mean|allowed|eligible|eligibility|fee|fees|penalty|refund|different specialist|another specialist)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final int STREAM_CHUNK_SIZE = 24;
    private static final long STREAM_CHUNK_DELAY_MS = 24L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> RESCHEDULABLE_STATUSES = Set.of("PENDING", "CONFIRMED");
    private static final Set<String> ABORT_PHRASES = Set.of(
            "forget it",
            "never mind",
            "nevermind",
            "cancel this process",
            "stop",
            "quit",
            "exit",
            "abort"
    );

    private final RescheduleTaskStateStore rescheduleTaskStateStore;
    private final RescheduleWorkflowAssistant rescheduleWorkflowAssistant;
    private final BookingService bookingService;
    private final SpecialistQueryService specialistQueryService;
    private final AiIntentRouterService aiIntentRouterService;
    private final WorkflowBookingIdentificationSupport bookingIdentificationSupport;

    public RescheduleWorkflowServiceImpl(
            RescheduleTaskStateStore rescheduleTaskStateStore,
            RescheduleWorkflowAssistant rescheduleWorkflowAssistant,
            BookingService bookingService,
            SpecialistQueryService specialistQueryService,
            AiIntentRouterService aiIntentRouterService,
            WorkflowBookingIdentificationSupport bookingIdentificationSupport
    ) {
        this.rescheduleTaskStateStore = rescheduleTaskStateStore;
        this.rescheduleWorkflowAssistant = rescheduleWorkflowAssistant;
        this.bookingService = bookingService;
        this.specialistQueryService = specialistQueryService;
        this.aiIntentRouterService = aiIntentRouterService;
        this.bookingIdentificationSupport = bookingIdentificationSupport;
    }

    @Override
    public boolean hasActiveTask(Long userId) {
        return rescheduleTaskStateStore.get(userId).isPresent();
    }

    @Override
    public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
        if (looksLikeRescheduleKnowledgeQuestion(originalUserMessage)) {
            return false;
        }
        if (looksLikeRescheduleRequest(originalUserMessage)) {
            return true;
        }
        return aiIntentRouterService.resolveIntent(userId, originalUserMessage) == AiIntent.BOOKING
                && looksLikeRescheduleRequest(originalUserMessage);
    }

    @Override
    public String handle(Long userId, String normalizedUserMessage) {
        String originalUserMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
        RescheduleTaskState state = rescheduleTaskStateStore.get(userId).orElseGet(this::createInitialState);
        return handleInternal(userId, originalUserMessage, state);
    }

    @Override
    public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
        String reply = handle(userId, normalizedUserMessage);
        return new SingleReplyTokenStream(reply);
    }

    private String handleInternal(Long userId, String originalUserMessage, RescheduleTaskState state) {
        if (isAbortShortcut(originalUserMessage)) {
            rescheduleTaskStateStore.clear(userId);
            return RESCHEDULE_TASK_ABORTED_MARKER + " Reschedule flow closed.";
        }

        if (state.getStep() == null) {
            state.setStep(RescheduleTaskState.Step.IDENTIFY);
        }

        if (state.getStep() == RescheduleTaskState.Step.IDENTIFY) {
            return handleIdentifyStep(userId, originalUserMessage, state);
        }

        return handlePreCheckAndTrigger(userId, originalUserMessage, state);
    }

    private String handleIdentifyStep(Long userId, String originalUserMessage, RescheduleTaskState state) {
        List<BookingItemVO> candidates = loadReschedulableBookings(userId);
        if (candidates.isEmpty()) {
            rescheduleTaskStateStore.clear(userId);
            return "You do not have any reschedulable bookings right now.";
        }

        WorkflowBookingIdentificationSupport.BookingIdentificationResult identificationResult =
                bookingIdentificationSupport.identifyBooking(
                        userId,
                        originalUserMessage,
                        Optional.ofNullable(state.getTaskStateText()).orElse("identifying booking to reschedule"),
                        candidates,
                        state.getCandidateBookingIds(),
                        rescheduleWorkflowAssistant::process
                );

        if (identificationResult.status() == WorkflowBookingIdentificationSupport.Status.ABORTED) {
            rescheduleTaskStateStore.clear(userId);
            return RESCHEDULE_TASK_ABORTED_MARKER + " Reschedule flow closed.";
        }

        if (identificationResult.status() == WorkflowBookingIdentificationSupport.Status.NEEDS_USER_SELECTION) {
            List<BookingItemVO> matchedBookings = identificationResult.matchedBookings().isEmpty()
                    ? candidates
                    : identificationResult.matchedBookings();
            state.setStep(RescheduleTaskState.Step.IDENTIFY);
            state.setCandidateBookingIds(matchedBookings.stream()
                    .map(BookingItemVO::getId)
                    .filter(Objects::nonNull)
                    .map(Long::valueOf)
                    .toList());
            state.setTaskStateText("waiting for the user to choose one booking to reschedule");
            state.setTargetDate(extractTargetDate(originalUserMessage, null));
            state.setRequestedTimeIntent(extractRequestedTimeIntent(originalUserMessage));
            state.setDisambiguationHint(identificationResult.message());
            rescheduleTaskStateStore.save(userId, state);
            return buildCandidatePrompt(matchedBookings, identificationResult.message());
        }

        state.setTargetBookingId(identificationResult.resolvedBookingId());
        state.setStep(RescheduleTaskState.Step.PRE_CHECK);
        state.setCandidateBookingIds(List.of(identificationResult.resolvedBookingId()));
        state.setTaskStateText("booking identified, checking availability and preparing reschedule trigger");
        state.setRequestedTimeIntent(extractRequestedTimeIntent(originalUserMessage));
        state.setDisambiguationHint(null);
        rescheduleTaskStateStore.save(userId, state);
        return handlePreCheckAndTrigger(userId, originalUserMessage, state);
    }

    private String handlePreCheckAndTrigger(Long userId, String originalUserMessage, RescheduleTaskState state) {
        Long bookingId = state.getTargetBookingId();
        if (bookingId == null) {
            rescheduleTaskStateStore.clear(userId);
            return RESCHEDULE_TASK_ABORTED_MARKER + " Reschedule flow lost the target booking. Please start again.";
        }

        BookingDetailVO bookingDetail = bookingService.getBookingDetailById(bookingId, userId);
        if (bookingDetail == null || bookingDetail.getSpecialistId() == null) {
            rescheduleTaskStateStore.clear(userId);
            return "Sorry, I could not load the booking details for rescheduling.";
        }

        String targetDate = resolveTargetDate(originalUserMessage, state, bookingDetail);
        state.setTargetDate(targetDate);

        List<SpecialistAvailabilityVO> availableSlots = specialistQueryService
                .listAvailability(bookingDetail.getSpecialistId(), LocalDate.parse(targetDate))
                .stream()
                .filter(Objects::nonNull)
                .filter(slot -> "AVAILABLE".equalsIgnoreCase(slot.getStatus()))
                .filter(this::slotStartsAfterTwoHours)
                .toList();

        Long suggestedSlotId = resolveSuggestedSlotId(availableSlots, state.getRequestedTimeIntent());
        state.setSuggestedSlotId(suggestedSlotId);

        if (suggestedSlotId != null) {
            BookingRescheduleQuoteVO quote;
            try {
                quote = bookingService.customerRescheduleQuote(bookingId, suggestedSlotId, userId);
            } catch (BusinessException exception) {
                rescheduleTaskStateStore.clear(userId);
                return safeText(exception.getMessage());
            }
            rescheduleTaskStateStore.clear(userId);
            if (!quote.isAllowed()) {
                return safeText(quote.getMessage());
            }
            return """
                    I have prepared the reschedule window for you. There is an available time on %s, and you can make the final confirmation in the popup.
                    %s%d:%s:%d]
                    """.formatted(targetDate, TRIGGER_RESCHEDULE_MODAL_PREFIX, bookingId, targetDate, suggestedSlotId);
        }

        rescheduleTaskStateStore.clear(userId);
        if (availableSlots.isEmpty()) {
            return """
                    This specialist has no available time slots on %s. You can still open the reschedule popup and choose an available time slot yourself.
                    %s%d:%s:]
                    """.formatted(targetDate, TRIGGER_RESCHEDULE_MODAL_PREFIX, bookingId, targetDate);
        }

        return """
                I have prepared the reschedule window for you. You can review the available time slots on %s and make the final confirmation in the popup.
                %s%d:%s:]
                """.formatted(targetDate, TRIGGER_RESCHEDULE_MODAL_PREFIX, bookingId, targetDate);
    }

    private RescheduleTaskState createInitialState() {
        return RescheduleTaskState.builder()
                .step(RescheduleTaskState.Step.IDENTIFY)
                .taskStateText("identifying which booking the user wants to reschedule")
                .build();
    }

    private boolean looksLikeRescheduleRequest(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = normalize(message);
        return RESCHEDULE_INTENT_PATTERN.matcher(normalized).find()
                || message.contains("改期")
                || message.contains("改时间")
                || message.contains("换时间")
                || message.contains("调整预约")
                || message.contains("重新预约");
    }

    private boolean looksLikeRescheduleKnowledgeQuestion(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = normalize(message);
        if (!looksLikeRescheduleRequest(message)) {
            return false;
        }
        return RESCHEDULE_KNOWLEDGE_QUESTION_PATTERN.matcher(normalized).find()
                || normalized.startsWith("can i ")
                || normalized.startsWith("could i ")
                || normalized.startsWith("do i ")
                || normalized.startsWith("does ")
                || normalized.startsWith("is it ")
                || normalized.startsWith("should i ");
    }

    private boolean isAbortShortcut(String originalUserMessage) {
        String normalized = normalize(originalUserMessage);
        for (String phrase : ABORT_PHRASES) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        return originalUserMessage != null && (originalUserMessage.contains("算了") || originalUserMessage.contains("退出"));
    }

    private List<BookingItemVO> loadReschedulableBookings(Long userId) {
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
                .filter(item -> item.getStatus() != null && RESCHEDULABLE_STATUSES.contains(item.getStatus().toUpperCase(Locale.ROOT)))
                .toList();
    }

    private String buildCandidatePrompt(List<BookingItemVO> candidates, String clarificationMessage) {
        List<String> lines = new ArrayList<>();
        if (clarificationMessage != null && !clarificationMessage.isBlank()) {
            lines.add(clarificationMessage);
        } else {
            lines.add("I found multiple reschedulable bookings. Please reply with the exact booking ID you want to reschedule.");
        }
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

    private String resolveTargetDate(String originalUserMessage, RescheduleTaskState state, BookingDetailVO bookingDetail) {
        String extracted = extractTargetDate(originalUserMessage, bookingDetail.getSlotDate());
        if (extracted != null) {
            return extracted;
        }
        if (state.getTargetDate() != null && !state.getTargetDate().isBlank()) {
            return state.getTargetDate();
        }
        return safeText(bookingDetail.getSlotDate());
    }

    private String extractTargetDate(String originalUserMessage, String fallbackDate) {
        String message = Optional.ofNullable(originalUserMessage).orElse("").trim();
        Matcher explicitDate = DATE_PATTERN.matcher(message);
        if (explicitDate.find()) {
            return explicitDate.group(1);
        }

        LocalDate base = parseDate(fallbackDate).orElse(LocalDate.now());
        String normalized = normalize(message);
        if (normalized.contains("today") || message.contains("今天")) {
            return base.toString();
        }
        if (normalized.contains("tomorrow") || message.contains("明天")) {
            return base.plusDays(1).toString();
        }
        if (normalized.contains("next monday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();
        }
        if (normalized.contains("next tuesday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).toString();
        }
        if (normalized.contains("next wednesday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).toString();
        }
        if (normalized.contains("next thursday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.THURSDAY)).toString();
        }
        if (normalized.contains("next friday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).toString();
        }
        if (normalized.contains("next saturday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).toString();
        }
        if (normalized.contains("next sunday")) {
            return base.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).toString();
        }
        if (normalized.contains("monday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).toString();
        }
        if (normalized.contains("tuesday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)).toString();
        }
        if (normalized.contains("wednesday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).toString();
        }
        if (normalized.contains("thursday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)).toString();
        }
        if (normalized.contains("friday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).toString();
        }
        if (normalized.contains("saturday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).toString();
        }
        if (normalized.contains("sunday")) {
            return base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toString();
        }
        return fallbackDate;
    }

    private String extractRequestedTimeIntent(String originalUserMessage) {
        String raw = Optional.ofNullable(originalUserMessage).orElse("");
        String normalized = normalize(originalUserMessage);
        if (normalized.contains("afternoon") || raw.contains("下午")) {
            return "afternoon";
        }
        if (normalized.contains("morning") || raw.contains("上午")) {
            return "morning";
        }
        if (normalized.contains("evening") || raw.contains("晚上")) {
            return "evening";
        }
        if (normalized.contains("around 3") || normalized.contains("3pm") || normalized.contains("3 pm")) {
            return "15:00";
        }
        boolean hasExplicitClockHint = normalized.matches(".*\\b\\d{1,2}:\\d{2}\\b.*")
                || normalized.matches(".*\\b\\d{1,2}\\s*(am|pm)\\b.*")
                || raw.contains("点")
                || normalized.contains("around ");
        if (!hasExplicitClockHint) {
            return null;
        }
        Matcher matcher = TIME_PATTERN.matcher(normalized);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            String minute = matcher.group(2) == null ? "00" : matcher.group(2);
            String meridiem = matcher.group(3);
            if ("pm".equalsIgnoreCase(meridiem) && hour < 12) {
                hour += 12;
            } else if ("am".equalsIgnoreCase(meridiem) && hour == 12) {
                hour = 0;
            }
            return "%02d:%s".formatted(hour, minute);
        }
        return null;
    }

    private Long resolveSuggestedSlotId(List<SpecialistAvailabilityVO> availableSlots, String requestedTimeIntent) {
        if (requestedTimeIntent == null || requestedTimeIntent.isBlank() || availableSlots.isEmpty()) {
            return null;
        }
        List<SpecialistAvailabilityVO> sorted = availableSlots.stream()
                .sorted(Comparator.comparing(SpecialistAvailabilityVO::getStartTime))
                .toList();
        return switch (requestedTimeIntent) {
            case "morning" -> sorted.stream()
                    .filter(slot -> slot.getStartTime() != null && slot.getStartTime().isBefore(LocalTime.NOON))
                    .map(SpecialistAvailabilityVO::getId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            case "afternoon" -> sorted.stream()
                    .filter(slot -> slot.getStartTime() != null
                            && !slot.getStartTime().isBefore(LocalTime.NOON)
                            && slot.getStartTime().isBefore(LocalTime.of(18, 0)))
                    .map(SpecialistAvailabilityVO::getId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            case "evening" -> sorted.stream()
                    .filter(slot -> slot.getStartTime() != null && !slot.getStartTime().isBefore(LocalTime.of(18, 0)))
                    .map(SpecialistAvailabilityVO::getId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            default -> resolveNearestByClockTime(sorted, requestedTimeIntent);
        };
    }

    private Long resolveNearestByClockTime(List<SpecialistAvailabilityVO> slots, String requestedTimeIntent) {
        try {
            LocalTime target = LocalTime.parse(requestedTimeIntent.length() == 5 ? requestedTimeIntent + ":00" : requestedTimeIntent);
            return slots.stream()
                    .filter(slot -> slot.getId() != null && slot.getStartTime() != null)
                    .min(Comparator.comparingLong(slot -> Math.abs(slot.getStartTime().toSecondOfDay() - target.toSecondOfDay())))
                    .map(SpecialistAvailabilityVO::getId)
                    .orElse(null);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private boolean slotStartsAfterTwoHours(SpecialistAvailabilityVO slot) {
        if (slot == null || slot.getSlotDate() == null || slot.getStartTime() == null) {
            return false;
        }
        return LocalDateTime.of(slot.getSlotDate(), slot.getStartTime()).isAfter(LocalDateTime.now().plusHours(2));
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim()));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private String safeText(Object value) {
        if (value == null) {
            return "N/A";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "N/A" : text;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
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

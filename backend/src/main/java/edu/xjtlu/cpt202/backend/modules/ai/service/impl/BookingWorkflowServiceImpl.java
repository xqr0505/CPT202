package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.BookingTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.BookingTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.BookingWorkflowService;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingAutoSubmitResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated hardcoded booking workflow.
 *
 * @author QiranXiao
 * @since 2026/5/5
 */
@Service
public class BookingWorkflowServiceImpl implements BookingWorkflowService {

    static final String BOOKING_TASK_ABORTED_MARKER = "[BOOKING_TASK_ABORTED]";
    static final String BOOKING_PREVIEW_MARKER = "AI_BOOKING_PREVIEW:";

    private static final int STREAM_CHUNK_SIZE = 24;
    private static final long STREAM_CHUNK_DELAY_MS = 24L;
    private static final int MAX_SPECIALIST_CANDIDATES = 8;
    private static final int MAX_SPECIALISTS_PER_PAGE = 24;
    private static final int MAX_SPECIALIST_SEARCH_PAGES = 3;
    private static final int MAX_SLOT_ROWS = 30;
    private static final int DEFAULT_NEAREST_DAYS = 3;
    private static final String USER_MESSAGE_MARKER = "User message:";

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,18})\\b");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[号日]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAY_OF_MONTH_PATTERN = Pattern.compile("(\\d{1,2})\\s*[号日]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTEXT_SPECIALIST_ID_PATTERN = Pattern.compile("specialistId\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPECIALIST_ID_PATTERN = Pattern.compile("specialist(?:\\s*id)?\\s*[:=]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTEXT_TOPIC_PATTERN = Pattern.compile("Current\\s+selected\\s+topic\\s+on\\s+page\\s*:\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTEXT_NOTES_PATTERN = Pattern.compile("Current\\s+selected\\s+customer\\s+notes\\s+on\\s+page\\s*:\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("topic\\s*[:=]\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOTES_PATTERN = Pattern.compile("notes?\\s*[:=]\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2}(?::\\d{2})?)\\s*-\\s*(\\d{1,2}:\\d{2}(?::\\d{2})?)");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b(\\d{1,2}:\\d{2}(?::\\d{2})?)\\b");
    private static final Pattern DR_NAME_PATTERN = Pattern.compile("\\bdr\\.?\\s+([a-z][a-z\\s'.-]{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCTOR_NAME_PATTERN = Pattern.compile("\\bdoctor\\s+([a-z][a-z\\s'.-]{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOK_NAME_PATTERN = Pattern.compile("\\bbook\\s+([a-z][a-z\\s'.-]{1,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_NAME_BEFORE_CN_EXPERT_PATTERN = Pattern.compile("([a-z][a-z\\s'.-]{1,40})\\s*(?:\\u533b\\u751f|\\u4e13\\u5bb6)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CN_SPECIALIST_NAME_PATTERN = Pattern.compile("(?:\\u533b\\u751f|\\u4e13\\u5bb6)\\s*([\\u4e00-\\u9fa5A-Za-z\\u00b7]{2,30})");
    private static final Pattern CN_SPECIALIST_NAME_SUFFIX_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z\\u00b7]{2,30})\\s*(?:\\u533b\\u751f|\\u4e13\\u5bb6)");
    private static final Pattern BOOKING_ORDER_PATTERN = Pattern.compile(
            "(\\bi\\s+want\\s+to\\s+book\\b|\\bbook\\s+(now|for\\s+me|this)\\b|\\bplace\\s+(a\\s+)?(booking|order)\\b|\\bsubmit\\s+(the\\s+)?booking\\b|\\bconfirm\\s+booking\\b|\\u4e0b\\u5355|\\u6211\\u8981\\u9884\\u7ea6|\\u5e2e\\u6211\\u9884\\u7ea6|\\u5e2e\\u6211\\u4e0b\\u5355|\\u63d0\\u4ea4\\u9884\\u7ea6|\\u786e\\u8ba4\\u9884\\u7ea6)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> DAWN_HINTS = Set.of("dawn", "midnight", "凌晨", "半夜", "深夜");
    private static final Set<String> MORNING_HINTS = Set.of("morning", "上午", "早上");
    private static final Set<String> AFTERNOON_HINTS = Set.of("afternoon", "下午");
    private static final Set<String> EVENING_HINTS = Set.of("evening", "night", "晚上", "傍晚");
    private static final Set<String> SPECIALIST_NAME_NOISE_HINTS = Set.of(
            "available", "availability", "slot", "time", "book", "booking",
            "dawn", "midnight", "morning", "afternoon", "evening", "night",
            "\u53ef\u7528", "\u65f6\u95f4", "\u65f6\u6bb5", "\u9884\u7ea6", "\u9884\u5b9a",
            "\u51cc\u6668", "\u534a\u591c", "\u6df1\u591c", "\u4e0a\u5348", "\u4e0b\u5348", "\u65e9\u4e0a", "\u665a\u4e0a", "\u50cd\u665a"
    );
    private static final Set<String> SPECIALIST_NAME_INVALID_CN_TOKENS = Set.of(
            "\u4e13\u5bb6", "\u533b\u751f", "\u7684", "\u53f7\u7684", "\u67d0\u4f4d", "\u4e00\u4f4d", "\u8fd9\u4e2a", "\u90a3\u4e2a"
    );
    private static final Pattern AM_TIME_PERIOD_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s*am\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PM_TIME_PERIOD_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s*pm\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> LEAVE_PHRASES = Set.of(
            "exit booking",
            "leave booking",
            "quit booking",
            "stop booking",
            "cancel booking workflow",
            "cancel this process",
            "i don't want to book",
            "i dont want to book",
            "don't book",
            "dont book",
            "no booking",
            "forget it",
            "never mind"
    );
    private static final Set<String> LEAVE_CN_HINTS = Set.of(
            "退出",
            "离开",
            "取消流程",
            "不订了",
            "不想订了",
            "不想预定了",
            "不想预约了",
            "不预定了",
            "不预约了",
            "先不订了",
            "暂时不订",
            "不要预定了",
            "不要预约了",
            "算了"
    );
    private static final Set<String> BOOKING_LOOKUP_HINTS = Set.of(
            "available", "availability", "free slot", "free time", "open slot",
            "bookable", "lookup", "query", "search",
            "可预约", "可用", "可用时间", "预约", "预定", "预约时间", "有空", "空余", "有时间", "时间段", "排期", "查询", "查找", "查看"
    );
    private static final Set<String> SPECIALIST_ENTITY_HINTS = Set.of(
            "specialist", "expert", "doctor", "dr", "医生", "专家"
    );
    private static final Set<String> GENERAL_CHITCHAT_HINTS = Set.of(
            "hello", "thanks", "thank you",
            "你好", "嗨", "哈喽", "在吗", "有人吗", "早上好", "上午好", "下午好", "晚上好", "谢谢"
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final BookingTaskStateStore bookingTaskStateStore;
    private final SpecialistQueryService specialistQueryService;
    private final BookingService bookingService;
    private final AiIntentRouterService aiIntentRouterService;

    public BookingWorkflowServiceImpl(
            BookingTaskStateStore bookingTaskStateStore,
            SpecialistQueryService specialistQueryService,
            BookingService bookingService,
            AiIntentRouterService aiIntentRouterService
    ) {
        this.bookingTaskStateStore = bookingTaskStateStore;
        this.specialistQueryService = specialistQueryService;
        this.bookingService = bookingService;
        this.aiIntentRouterService = aiIntentRouterService;
    }

    @Override
    public boolean hasActiveTask(Long userId) {
        return bookingTaskStateStore.get(userId).isPresent();
    }

    @Override
    public boolean shouldStartWorkflow(Long userId, String originalUserMessage) {
        String latestUserMessage = extractLatestUserUtterance(originalUserMessage);
        if (latestUserMessage.isBlank()) {
            return false;
        }
        if (aiIntentRouterService.resolveIntent(userId, latestUserMessage) == AiIntent.BOOKING) {
            return true;
        }
        if (looksLikeBookingOrder(latestUserMessage)) {
            return true;
        }
        return looksLikeBookingLookup(latestUserMessage);
    }

    @Override
    public String handle(Long userId, String normalizedUserMessage) {
        String originalUserMessage = ParallelToolAssistant.extractOriginalUserMessage(normalizedUserMessage);
        String latestUserMessage = extractLatestUserUtterance(originalUserMessage);
        BookingTaskState state = bookingTaskStateStore.get(userId).orElseGet(this::createInitialState);
        return handleInternal(userId, originalUserMessage, latestUserMessage, state);
    }

    @Override
    public TokenStream streamHandle(Long userId, String normalizedUserMessage) {
        String reply = handle(userId, normalizedUserMessage);
        return new SingleReplyTokenStream(reply);
    }

    private String handleInternal(
            Long userId,
            String originalUserMessage,
            String latestUserMessage,
            BookingTaskState state
    ) {
        if (isLeaveRequest(latestUserMessage)) {
            bookingTaskStateStore.clear(userId);
            return BOOKING_TASK_ABORTED_MARKER + " Booking flow closed.";
        }

        String preferredTopic = firstNonBlank(
                extractFirstGroup(TOPIC_PATTERN, latestUserMessage),
                extractFirstGroup(CONTEXT_TOPIC_PATTERN, originalUserMessage),
                state.getPreferredTopic()
        );
        String customerNotes = firstNonBlank(
                extractFirstGroup(NOTES_PATTERN, latestUserMessage),
                extractFirstGroup(CONTEXT_NOTES_PATTERN, originalUserMessage),
                state.getCustomerNotes()
        );
        state.setPreferredTopic(preferredTopic);
        state.setCustomerNotes(customerNotes);

        if (state.getCandidateSlots() != null && !state.getCandidateSlots().isEmpty()) {
            Long chosenSlotId = findChosenSlotId(latestUserMessage, state.getCandidateSlots());
            if (chosenSlotId != null) {
                BookingTaskState.CandidateSlotState chosen = state.getCandidateSlots().stream()
                        .filter(slot -> Objects.equals(slot.getSlotId(), chosenSlotId))
                        .findFirst()
                        .orElse(null);
                if (chosen != null) {
                    bookingTaskStateStore.clear(userId);
                    return buildBookingPreview(userId, chosen, preferredTopic, customerNotes);
                }
            }
            if (!containsNewSearchSignal(latestUserMessage)) {
                if (isGeneralChatWithoutBookingIntent(latestUserMessage)) {
                    bookingTaskStateStore.clear(userId);
                    return BOOKING_TASK_ABORTED_MARKER + " Booking flow closed.";
                }
                state.setTaskStateText("waiting for the user to pick a slot id");
                bookingTaskStateStore.save(userId, state);
                return buildSlotSelectionPrompt(state.getCandidateSlots(), "Please reply with the Slot ID you want to book.");
            }
            state.setCandidateSlots(null);
        }

        BookingRequest request = extractBookingRequest(originalUserMessage, latestUserMessage);
        boolean hasSpecialist = request.specialistId() != null
                || (request.specialistName() != null && !request.specialistName().isBlank());
        boolean hasDate = request.slotDate() != null;

        if (!hasSpecialist && !hasDate) {
            state.setTaskStateText("waiting for specialist name");
            bookingTaskStateStore.save(userId, state);
            return """
                    I am in booking workflow now.
                    You can start with either:
                    1) specialist name (I will show nearest 3-day availability), or
                    2) date + time period (凌晨/上午/下午/晚上, I will find suitable specialists).

                    Examples:
                    - "Book Dr. Emily Chen"
                    - "I want to book 2026-05-20 evening"
                    """;
        }

        if (hasSpecialist && !hasDate) {
            List<BookingTaskState.CandidateSlotState> nearestCandidates = loadNearestThreeDayCandidates(request);
            if (nearestCandidates.isEmpty()) {
                state.setTaskStateText("no available slots found in nearest three days");
                bookingTaskStateStore.save(userId, state);
                return "I could not find available slots for this specialist in the next 3 days. Please provide another specialist name or specific date.";
            }
            state.setCandidateSlots(nearestCandidates);
            state.setTaskStateText("waiting for the user to pick a slot id");
            bookingTaskStateStore.save(userId, state);
            return buildSlotSelectionPrompt(
                    nearestCandidates,
                    "I found available slots in the next 3 days. Please pick Slot ID to continue booking."
            );
        }

        List<BookingTaskState.CandidateSlotState> candidates = loadCandidates(request);
        if (candidates.isEmpty()
                && !hasSpecialist
                && request.slotDate() != null
                && request.timePeriod() != null
                && request.timePeriod() != TimePeriod.ANY) {
            BookingRequest fallbackAnyPeriodRequest = new BookingRequest(
                    request.specialistId(),
                    request.specialistName(),
                    request.slotId(),
                    request.slotDate(),
                    request.startTime(),
                    request.endTime(),
                    TimePeriod.ANY
            );
            List<BookingTaskState.CandidateSlotState> fallbackCandidates = loadCandidates(fallbackAnyPeriodRequest);
            if (!fallbackCandidates.isEmpty()) {
                state.setCandidateSlots(fallbackCandidates);
                state.setTaskStateText("requested time period not found, waiting for slot id from all-day candidates");
                bookingTaskStateStore.save(userId, state);
                String requestedPeriodLabel = toDisplayPeriod(request.timePeriod());
                String availablePeriodLabel = summarizeAvailablePeriods(fallbackCandidates);
                return buildSlotSelectionPrompt(
                        fallbackCandidates,
                        "没有%s时段，但是这天%s有空余，是否继续预订？请回复 Slot ID。"
                                .formatted(requestedPeriodLabel, availablePeriodLabel)
                );
            }
        }
        if (candidates.isEmpty()) {
            state.setTaskStateText("no available slots found, waiting for another query");
            bookingTaskStateStore.save(userId, state);
            if (!hasSpecialist) {
                return "I could not find suitable specialists for that date/time preference. Please try another date or time period.";
            }
            return "I could not find available slots for that specialist/date. Please provide another specialist name or date.";
        }

        List<BookingTaskState.CandidateSlotState> matched = resolveMatchedCandidates(request, candidates);
        if (request.slotId() != null) {
            if (matched.size() == 1) {
                bookingTaskStateStore.clear(userId);
                return buildBookingPreview(userId, matched.get(0), preferredTopic, customerNotes);
            }
            state.setCandidateSlots(candidates);
            state.setTaskStateText("slot id was not valid, waiting for the user to pick a slot id");
            bookingTaskStateStore.save(userId, state);
            return buildSlotSelectionPrompt(candidates, "That slotId is not available. Please pick one Slot ID from the table below.");
        }

        if (request.startTime() != null) {
            if (matched.size() == 1) {
                bookingTaskStateStore.clear(userId);
                return buildBookingPreview(userId, matched.get(0), preferredTopic, customerNotes);
            }
            if (matched.isEmpty()) {
                state.setCandidateSlots(candidates);
                state.setTaskStateText("time did not match any slot, waiting for slot id");
                bookingTaskStateStore.save(userId, state);
                return buildSlotSelectionPrompt(candidates, "No exact slot matched that time. Please pick Slot ID from the table.");
            }
            state.setCandidateSlots(matched);
            state.setTaskStateText("multiple slots matched time, waiting for slot id");
            bookingTaskStateStore.save(userId, state);
            return buildSlotSelectionPrompt(matched, "I found multiple matching slots. Please pick Slot ID.");
        }

        state.setCandidateSlots(candidates);
        state.setTaskStateText("waiting for the user to pick a slot id");
        bookingTaskStateStore.save(userId, state);
        return buildSlotSelectionPrompt(candidates, "I found available slots. Please pick Slot ID to continue booking.");
    }

    private List<BookingTaskState.CandidateSlotState> loadCandidates(BookingRequest request) {
        if (request.slotDate() == null) {
            return List.of();
        }
        if (request.specialistId() != null) {
            List<BookingTaskState.CandidateSlotState> byId = loadCandidatesForSpecialist(request.specialistId(), request.slotDate(), null);
            return applyTimePeriodFilter(byId, request.timePeriod(), request.startTime());
        }

        if (request.specialistName() == null || request.specialistName().isBlank()) {
            return loadCandidatesWithoutSpecialistName(request);
        }

        List<BookingTaskState.CandidateSlotState> candidates = new ArrayList<>();
        SpecialistSearchQueryDTO queryDTO = new SpecialistSearchQueryDTO();
        queryDTO.setKeyword(request.specialistName());
        queryDTO.setDate(request.slotDate());
        queryDTO.setPageNo(1);
        queryDTO.setPageSize(MAX_SPECIALIST_CANDIDATES);
        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(queryDTO);
        if (result == null || result.getList() == null) {
            return List.of();
        }
        for (SpecialistSummaryVO specialist : result.getList()) {
            if (specialist == null || specialist.getId() == null) {
                continue;
            }
            candidates.addAll(loadCandidatesForSpecialist(
                    specialist.getId(),
                    request.slotDate(),
                    specialist.getConsultationFee()
            ));
        }
        return applyTimePeriodFilter(candidates, request.timePeriod(), request.startTime())
                .stream()
                .sorted(Comparator
                        .comparing(BookingTaskState.CandidateSlotState::getSlotDate, Comparator.nullsLast(String::compareTo))
                        .thenComparing(BookingTaskState.CandidateSlotState::getStartTime, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_SLOT_ROWS)
                .toList();
    }

    private List<BookingTaskState.CandidateSlotState> loadCandidatesWithoutSpecialistName(BookingRequest request) {
        if (request.slotDate() == null) {
            return List.of();
        }
        List<BookingTaskState.CandidateSlotState> candidates = new ArrayList<>();
        Set<Long> processedSpecialistIds = new java.util.LinkedHashSet<>();

        for (int pageNo = 1; pageNo <= MAX_SPECIALIST_SEARCH_PAGES; pageNo++) {
            SpecialistSearchQueryDTO queryDTO = new SpecialistSearchQueryDTO();
            queryDTO.setDate(request.slotDate());
            queryDTO.setPageNo(pageNo);
            queryDTO.setPageSize(MAX_SPECIALISTS_PER_PAGE);
            PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(queryDTO);
            if (result == null || result.getList() == null || result.getList().isEmpty()) {
                break;
            }

            for (SpecialistSummaryVO specialist : result.getList()) {
                if (specialist == null || specialist.getId() == null || processedSpecialistIds.contains(specialist.getId())) {
                    continue;
                }
                processedSpecialistIds.add(specialist.getId());
                candidates.addAll(loadCandidatesForSpecialist(
                        specialist.getId(),
                        request.slotDate(),
                        specialist.getConsultationFee()
                ));
            }
        }

        return applyTimePeriodFilter(candidates, request.timePeriod(), request.startTime())
                .stream()
                .sorted(Comparator
                        .comparing(BookingTaskState.CandidateSlotState::getSlotDate, Comparator.nullsLast(String::compareTo))
                        .thenComparing(BookingTaskState.CandidateSlotState::getStartTime, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_SLOT_ROWS)
                .toList();
    }

    private List<BookingTaskState.CandidateSlotState> loadNearestThreeDayCandidates(BookingRequest request) {
        List<BookingTaskState.CandidateSlotState> candidates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < DEFAULT_NEAREST_DAYS; offset++) {
            LocalDate date = today.plusDays(offset);
            BookingRequest dateRequest = new BookingRequest(
                    request.specialistId(),
                    request.specialistName(),
                    null,
                    date,
                    request.startTime(),
                    request.endTime(),
                    request.timePeriod()
            );
            candidates.addAll(loadCandidates(dateRequest));
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing(BookingTaskState.CandidateSlotState::getSlotDate, Comparator.nullsLast(String::compareTo))
                        .thenComparing(BookingTaskState.CandidateSlotState::getStartTime, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_SLOT_ROWS)
                .toList();
    }

    private List<BookingTaskState.CandidateSlotState> loadCandidatesForSpecialist(
            Long specialistId,
            LocalDate slotDate,
            java.math.BigDecimal fallbackFee
    ) {
        SpecialistDetailVO detail;
        try {
            detail = specialistQueryService.getSpecialistDetail(specialistId);
        } catch (BusinessException exception) {
            return List.of();
        }
        if (detail == null || !"ACTIVE".equalsIgnoreCase(detail.getStatus())) {
            return List.of();
        }

        return Optional.ofNullable(specialistQueryService.listAvailability(specialistId, slotDate))
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(slot -> TimeSlotStatusEnum.AVAILABLE.name().equalsIgnoreCase(slot.getStatus()))
                .map(slot -> BookingTaskState.CandidateSlotState.builder()
                        .specialistId(specialistId)
                        .specialistName(detail.getName())
                        .consultationFee(detail.getConsultationFee() == null ? fallbackFee : detail.getConsultationFee())
                        .slotId(slot.getId())
                        .slotDate(slot.getSlotDate() == null ? null : slot.getSlotDate().toString())
                        .startTime(slot.getStartTime() == null ? null : slot.getStartTime().toString())
                        .endTime(slot.getEndTime() == null ? null : slot.getEndTime().toString())
                        .build())
                .toList();
    }

    private List<BookingTaskState.CandidateSlotState> resolveMatchedCandidates(
            BookingRequest request,
            List<BookingTaskState.CandidateSlotState> candidates
    ) {
        if (request.slotId() != null) {
            return candidates.stream()
                    .filter(item -> Objects.equals(item.getSlotId(), request.slotId()))
                    .toList();
        }
        if (request.startTime() == null) {
            return candidates;
        }
        return candidates.stream()
                .filter(item -> Objects.equals(parseTime(item.getStartTime()), request.startTime()))
                .filter(item -> request.endTime() == null || Objects.equals(parseTime(item.getEndTime()), request.endTime()))
                .toList();
    }

    private List<BookingTaskState.CandidateSlotState> applyTimePeriodFilter(
            List<BookingTaskState.CandidateSlotState> candidates,
            TimePeriod timePeriod,
            LocalTime exactStartTime
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (exactStartTime != null || timePeriod == null || timePeriod == TimePeriod.ANY) {
            return candidates;
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> {
                    LocalTime start = parseTime(candidate.getStartTime());
                    if (start == null) {
                        return false;
                    }
                    TimePeriod period = classifyTimePeriod(start);
                    return period == timePeriod || timePeriod == TimePeriod.ANY;
                })
                .toList();
    }

    private String buildBookingPreview(
            Long userId,
            BookingTaskState.CandidateSlotState slot,
            String preferredTopic,
            String customerNotes
    ) {
        if (slot == null || slot.getSpecialistId() == null || slot.getSlotId() == null || slot.getSlotDate() == null) {
            return "I could not build booking preview from the selected slot. Please try again.";
        }

        LocalDate slotDate = parseDate(slot.getSlotDate());
        LocalTime startTime = parseTime(slot.getStartTime());
        LocalTime endTime = parseTime(slot.getEndTime());
        if (slotDate == null || startTime == null) {
            return "Selected slot has invalid date/time. Please choose another slot ID.";
        }

        AiBookingFormDraftVO draft = bookingService.buildAiBookingDraft(
                userId,
                slot.getSpecialistId(),
                slot.getSlotId(),
                preferredTopic,
                customerNotes
        );

        AiBookingAutoSubmitResultVO preview = AiBookingAutoSubmitResultVO.builder()
                .success(draft.getTopic() != null && !draft.getTopic().isBlank())
                .readyToSubmit(draft.getTopic() != null && !draft.getTopic().isBlank())
                .message(draft.getTopic() != null && !draft.getTopic().isBlank()
                        ? "Booking draft prepared. Please confirm submission in UI."
                        : "Please choose a topic from dropdown and fill notes in booking popup.")
                .specialistId(slot.getSpecialistId())
                .slotId(slot.getSlotId())
                .slotDate(slotDate)
                .startTime(startTime)
                .endTime(endTime)
                .specialistName(slot.getSpecialistName())
                .consultationFee(slot.getConsultationFee())
                .topic(draft.getTopic())
                .customerNotes(draft.getCustomerNotes())
                .availableTopics(draft.getAvailableTopics())
                .warnings(draft.getWarnings())
                .build();

        if (!preview.isReadyToSubmit()) {
            return """
                    I prepared a draft but it is not ready to submit yet.

                    %s%s
                    """.formatted(BOOKING_PREVIEW_MARKER, toJson(preview));
        }

        return """
                Please review the booking draft in popup and confirm manually.

                %s%s
                """.formatted(BOOKING_PREVIEW_MARKER, toJson(preview));
    }

    private String buildSlotSelectionPrompt(List<BookingTaskState.CandidateSlotState> candidates, String leadText) {
        List<String> lines = new ArrayList<>();
        lines.add(leadText);
        lines.add("");
        lines.add("| Specialist ID | Specialist | Date | Slot ID | Start | End | Fee |");
        lines.add("| --- | --- | --- | --- | --- | --- | --- |");
        for (BookingTaskState.CandidateSlotState slot : candidates.stream().limit(MAX_SLOT_ROWS).toList()) {
            lines.add("| %s | %s | %s | %s | %s | %s | %s |".formatted(
                    safeText(slot.getSpecialistId()),
                    safeText(slot.getSpecialistName()),
                    safeText(slot.getSlotDate()),
                    safeText(slot.getSlotId()),
                    safeText(slot.getStartTime()),
                    safeText(slot.getEndTime()),
                    slot.getConsultationFee() == null ? "N/A" : slot.getConsultationFee().toPlainString()
            ));
        }
        lines.add("");
        lines.add("Reply with Slot ID only, for example: 12345");
        return String.join("\n", lines);
    }

    private Long findChosenSlotId(String latestUserMessage, List<BookingTaskState.CandidateSlotState> candidates) {
        Set<String> candidateIds = candidates.stream()
                .map(BookingTaskState.CandidateSlotState::getSlotId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toSet());
        Matcher matcher = NUMBER_PATTERN.matcher(Optional.ofNullable(latestUserMessage).orElse(""));
        while (matcher.find()) {
            String value = matcher.group(1);
            if (candidateIds.contains(value)) {
                return Long.valueOf(value);
            }
        }
        return null;
    }

    private BookingTaskState createInitialState() {
        return BookingTaskState.builder()
                .taskStateText("booking workflow active")
                .build();
    }

    private String toJson(AiBookingAutoSubmitResultVO payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private BookingRequest extractBookingRequest(String originalUserMessage, String latestUserMessage) {
        Long specialistId = firstPositiveLong(
                extractNumber(CONTEXT_SPECIALIST_ID_PATTERN, originalUserMessage),
                extractNumber(SPECIALIST_ID_PATTERN, latestUserMessage)
        );
        Long slotId = extractNumber(Pattern.compile("slot(?:\\s*id)?\\s*[:=]\\s*(\\d+)", Pattern.CASE_INSENSITIVE), latestUserMessage);

        String specialistName = firstNonBlank(
                cleanupEnglishName(extractFirstGroup(EN_NAME_BEFORE_CN_EXPERT_PATTERN, latestUserMessage)),
                sanitizeSpecialistNameCandidate(extractFirstGroup(CN_SPECIALIST_NAME_SUFFIX_PATTERN, latestUserMessage)),
                sanitizeSpecialistNameCandidate(extractFirstGroup(CN_SPECIALIST_NAME_PATTERN, latestUserMessage)),
                cleanupEnglishName(extractFirstGroup(DR_NAME_PATTERN, latestUserMessage)),
                cleanupEnglishName(extractFirstGroup(DOCTOR_NAME_PATTERN, latestUserMessage)),
                cleanupEnglishName(extractFirstGroup(BOOK_NAME_PATTERN, latestUserMessage))
        );
        LocalDate slotDate = resolveDate(originalUserMessage, latestUserMessage);
        String[] range = extractTimeRange(TIME_RANGE_PATTERN, latestUserMessage);
        LocalTime startTime = parseTime(firstNonBlank(range[0], extractFirstGroup(TIME_PATTERN, latestUserMessage)));
        LocalTime endTime = parseTime(range[1]);
        TimePeriod timePeriod = resolveTimePeriod(latestUserMessage);

        return new BookingRequest(specialistId, specialistName, slotId, slotDate, startTime, endTime, timePeriod);
    }

    private LocalDate resolveDate(String originalUserMessage, String latestUserMessage) {
        String explicit = firstNonBlank(
                extractFirstGroup(DATE_PATTERN, latestUserMessage),
                extractFirstGroup(DATE_PATTERN, originalUserMessage)
        );
        if (explicit != null) {
            return parseDate(explicit);
        }

        LocalDate monthDayDate = resolveMonthDayDate(latestUserMessage);
        if (monthDayDate != null) {
            return monthDayDate;
        }

        LocalDate dayOfMonthDate = resolveDayOfMonthDate(latestUserMessage);
        if (dayOfMonthDate != null) {
            return dayOfMonthDate;
        }

        String normalized = normalize(latestUserMessage);
        LocalDate today = LocalDate.now();
        if (normalized.contains("today") || latestUserMessage.contains("\u4eca\u5929")) {
            return today;
        }
        if (normalized.contains("tomorrow") || latestUserMessage.contains("\u660e\u5929")) {
            return today.plusDays(1);
        }
        if (latestUserMessage.contains("\u540e\u5929")) {
            return today.plusDays(2);
        }
        if (normalized.contains("next monday")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        if (normalized.contains("next tuesday")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        }
        if (normalized.contains("next wednesday")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        }
        if (normalized.contains("next thursday")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        }
        if (normalized.contains("next friday")) {
            return today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        }
        return null;
    }

    private LocalDate resolveMonthDayDate(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return null;
        }
        Matcher matcher = MONTH_DAY_PATTERN.matcher(latestUserMessage);
        if (!matcher.find()) {
            return null;
        }

        try {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            LocalDate today = LocalDate.now();
            LocalDate date = LocalDate.of(today.getYear(), month, day);
            if (date.isBefore(today)) {
                date = date.plusYears(1);
            }
            return date;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDate resolveDayOfMonthDate(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return null;
        }
        Matcher matcher = DAY_OF_MONTH_PATTERN.matcher(latestUserMessage);
        if (!matcher.find()) {
            return null;
        }
        if (latestUserMessage.contains("月")) {
            return null;
        }

        try {
            int day = Integer.parseInt(matcher.group(1));
            LocalDate today = LocalDate.now();
            LocalDate baseMonth = LocalDate.of(today.getYear(), today.getMonth(), 1);
            LocalDate date = baseMonth.withDayOfMonth(day);
            if (date.isBefore(today)) {
                LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);
                date = nextMonth.withDayOfMonth(day);
            }
            return date;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private TimePeriod resolveTimePeriod(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return TimePeriod.ANY;
        }
        String normalized = normalize(latestUserMessage);
        Integer amHour = extractAmPmHour(AM_TIME_PERIOD_PATTERN, normalized);
        if (amHour != null) {
            int normalizedHour = amHour % 12;
            return normalizedHour < 6 ? TimePeriod.DAWN : TimePeriod.MORNING;
        }
        Integer pmHour = extractAmPmHour(PM_TIME_PERIOD_PATTERN, normalized);
        if (pmHour != null) {
            int normalizedHour = (pmHour % 12) + 12;
            return normalizedHour < 18 ? TimePeriod.AFTERNOON : TimePeriod.EVENING;
        }
        if (containsAny(normalized, DAWN_HINTS)) {
            return TimePeriod.DAWN;
        }
        if (containsAny(normalized, MORNING_HINTS)) {
            return TimePeriod.MORNING;
        }
        if (containsAny(normalized, AFTERNOON_HINTS)) {
            return TimePeriod.AFTERNOON;
        }
        if (containsAny(normalized, EVENING_HINTS)) {
            return TimePeriod.EVENING;
        }
        return TimePeriod.ANY;
    }

    private Integer extractAmPmHour(Pattern pattern, String source) {
        String hourText = extractFirstGroup(pattern, source);
        if (hourText == null || hourText.isBlank()) {
            return null;
        }
        try {
            int hour = Integer.parseInt(hourText);
            return (hour >= 0 && hour <= 12) ? hour : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private TimePeriod classifyTimePeriod(LocalTime startTime) {
        if (startTime == null) {
            return TimePeriod.ANY;
        }
        if (startTime.isBefore(LocalTime.of(6, 0))) {
            return TimePeriod.DAWN;
        }
        if (startTime.isBefore(LocalTime.NOON)) {
            return TimePeriod.MORNING;
        }
        if (startTime.isBefore(LocalTime.of(18, 0))) {
            return TimePeriod.AFTERNOON;
        }
        return TimePeriod.EVENING;
    }

    private String toDisplayPeriod(TimePeriod period) {
        if (period == null) {
            return "全天";
        }
        return switch (period) {
            case DAWN -> "凌晨";
            case MORNING -> "上午（早上）";
            case AFTERNOON -> "下午";
            case EVENING -> "晚上";
            case ANY -> "全天";
        };
    }

    private String summarizeAvailablePeriods(List<BookingTaskState.CandidateSlotState> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "其他时段";
        }
        EnumSet<TimePeriod> periods = EnumSet.noneOf(TimePeriod.class);
        for (BookingTaskState.CandidateSlotState candidate : candidates) {
            LocalTime start = parseTime(candidate == null ? null : candidate.getStartTime());
            TimePeriod period = classifyTimePeriod(start);
            if (period != null && period != TimePeriod.ANY) {
                periods.add(period);
            }
        }
        List<TimePeriod> order = List.of(TimePeriod.DAWN, TimePeriod.MORNING, TimePeriod.AFTERNOON, TimePeriod.EVENING);
        List<String> labels = order.stream()
                .filter(periods::contains)
                .map(this::toDisplayPeriod)
                .toList();
        return labels.isEmpty() ? "其他时段" : String.join("、", labels);
    }

    private boolean looksLikeBookingOrder(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return false;
        }
        return BOOKING_ORDER_PATTERN.matcher(latestUserMessage).find();
    }

    private boolean looksLikeBookingLookup(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return false;
        }

        String normalized = normalize(latestUserMessage);
        boolean hasLookupIntent = containsAny(normalized, BOOKING_LOOKUP_HINTS);
        if (!hasLookupIntent) {
            return false;
        }

        boolean hasSpecialistSignal = extractFirstGroup(EN_NAME_BEFORE_CN_EXPERT_PATTERN, latestUserMessage) != null
                || extractFirstGroup(CN_SPECIALIST_NAME_SUFFIX_PATTERN, latestUserMessage) != null
                || extractFirstGroup(CN_SPECIALIST_NAME_PATTERN, latestUserMessage) != null
                || extractFirstGroup(DR_NAME_PATTERN, latestUserMessage) != null
                || extractFirstGroup(DOCTOR_NAME_PATTERN, latestUserMessage) != null
                || containsAny(normalized, SPECIALIST_ENTITY_HINTS);

        boolean hasDateOrTimeSignal = resolveDate(latestUserMessage, latestUserMessage) != null
                || extractFirstGroup(TIME_PATTERN, latestUserMessage) != null
                || containsAny(normalized, DAWN_HINTS)
                || containsAny(normalized, MORNING_HINTS)
                || containsAny(normalized, AFTERNOON_HINTS)
                || containsAny(normalized, EVENING_HINTS);

        return hasSpecialistSignal || hasDateOrTimeSignal;
    }

    private boolean isGeneralChatWithoutBookingIntent(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return false;
        }
        if (looksLikeBookingOrder(latestUserMessage)
                || looksLikeBookingLookup(latestUserMessage)
                || isLeaveRequest(latestUserMessage)
                || containsNewSearchSignal(latestUserMessage)) {
            return false;
        }
        String normalized = normalize(latestUserMessage);
        return containsAny(normalized, GENERAL_CHITCHAT_HINTS);
    }

    private boolean isLeaveRequest(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return false;
        }
        String normalized = normalize(latestUserMessage);
        for (String phrase : LEAVE_PHRASES) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        for (String hint : LEAVE_CN_HINTS) {
            if (latestUserMessage.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNewSearchSignal(String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.isBlank()) {
            return false;
        }
        String normalized = normalize(latestUserMessage);
        return extractFirstGroup(DATE_PATTERN, latestUserMessage) != null
                || extractFirstGroup(MONTH_DAY_PATTERN, latestUserMessage) != null
                || extractFirstGroup(DAY_OF_MONTH_PATTERN, latestUserMessage) != null
                || extractFirstGroup(TIME_PATTERN, latestUserMessage) != null
                || extractFirstGroup(DR_NAME_PATTERN, latestUserMessage) != null
                || extractFirstGroup(DOCTOR_NAME_PATTERN, latestUserMessage) != null
                || extractFirstGroup(CN_SPECIALIST_NAME_PATTERN, latestUserMessage) != null
                || containsAny(normalized, DAWN_HINTS)
                || containsAny(normalized, MORNING_HINTS)
                || containsAny(normalized, AFTERNOON_HINTS)
                || containsAny(normalized, EVENING_HINTS)
                || latestUserMessage.contains("\u6362")
                || normalized.contains("another")
                || normalized.contains("other");
    }

    private String extractLatestUserUtterance(String originalUserMessage) {
        if (originalUserMessage == null || originalUserMessage.isBlank()) {
            return "";
        }
        int markerIndex = originalUserMessage.lastIndexOf(USER_MESSAGE_MARKER);
        if (markerIndex < 0) {
            return originalUserMessage.trim();
        }
        String extracted = originalUserMessage.substring(markerIndex + USER_MESSAGE_MARKER.length()).trim();
        return extracted.isBlank() ? originalUserMessage.trim() : extracted;
    }

    private String sanitizeSpecialistNameCandidate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim()
                .replaceAll("^[,，。.!?;；:：\\s]+", "")
                .replaceAll("[,，。.!?;；:：\\s]+$", "")
                .replaceAll("^(\\u5e2e\\u6211|\\u8bf7|\\u9ebb\\u70e6|\\u770b\\u770b|\\u6211\\u60f3|\\u6211\\u8981|\\u60f3\\u8981)+", "")
                .trim();
        if (cleaned.isBlank()) {
            return null;
        }
        if (cleaned.matches(".*\\d.*")) {
            return null;
        }
        if (cleaned.contains("\u6708") || cleaned.contains("\u53f7") || cleaned.contains("\u65e5")) {
            return null;
        }
        if (SPECIALIST_NAME_INVALID_CN_TOKENS.contains(cleaned)) {
            return null;
        }

        String normalized = normalize(cleaned);
        for (String hint : SPECIALIST_NAME_NOISE_HINTS) {
            if (hint != null && !hint.isBlank() && normalized.contains(hint.toLowerCase(Locale.ROOT))) {
                return null;
            }
        }
        return cleaned;
    }

    private String cleanupEnglishName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim()
                .replaceAll("\\b(today|tomorrow|next|at|on|for|this)\\b.*$", "")
                .replaceAll("\\d{1,2}:\\d{2}.*$", "")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean containsAny(String normalizedText, Set<String> hints) {
        if (normalizedText == null || normalizedText.isBlank() || hints == null || hints.isEmpty()) {
            return false;
        }
        for (String hint : hints) {
            if (hint != null && !hint.isBlank() && normalizedText.contains(hint.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Long firstPositiveLong(Long... candidates) {
        if (candidates == null) {
            return null;
        }
        for (Long candidate : candidates) {
            if (candidate != null && candidate > 0) {
                return candidate;
            }
        }
        return null;
    }

    private Long extractNumber(Pattern pattern, String source) {
        String value = extractFirstGroup(pattern, source);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String extractFirstGroup(Pattern pattern, String source) {
        if (pattern == null || source == null || source.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find() || matcher.groupCount() < 1) {
            return null;
        }
        String value = matcher.group(1);
        return value == null ? null : value.trim();
    }

    private String[] extractTimeRange(Pattern pattern, String source) {
        if (pattern == null || source == null || source.isBlank()) {
            return new String[] { null, null };
        }
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return new String[] { null, null };
        }
        String left = matcher.groupCount() >= 1 ? matcher.group(1) : null;
        String right = matcher.groupCount() >= 2 ? matcher.group(2) : null;
        return new String[] {
                left == null ? null : left.trim(),
                right == null ? null : right.trim()
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.matches("^\\d{1,2}:\\d{2}$")) {
            normalized = normalized + ":00";
        }
        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String safeText(Object value) {
        if (value == null) {
            return "N/A";
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "N/A" : text;
    }

    private record BookingRequest(
            Long specialistId,
            String specialistName,
            Long slotId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            TimePeriod timePeriod
    ) {
    }

    private enum TimePeriod {
        ANY,
        DAWN,
        MORNING,
        AFTERNOON,
        EVENING
    }

    private static class SingleReplyTokenStream implements TokenStream {

        private final String reply;
        private java.util.function.Consumer<String> onNext = ignored -> { };
        private java.util.function.Consumer<Response<AiMessage>> onComplete = ignored -> { };
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
        public TokenStream onComplete(java.util.function.Consumer<Response<AiMessage>> consumer) {
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
                onComplete.accept(Response.from(AiMessage.from(reply)));
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

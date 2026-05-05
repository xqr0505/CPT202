package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class WorkflowBookingIdentificationSupport {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(\\d{1,18})\\b");
    private static final Pattern ACTION_PATTERN = Pattern.compile("^ACTION\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("^BOOKING_ID\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern EXPERT_NAME_PATTERN = Pattern.compile("^EXPERT_NAME\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern CATEGORY_NAME_PATTERN = Pattern.compile("^CATEGORY_NAME\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern STATUS_PATTERN = Pattern.compile("^STATUS\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern START_DATE_PATTERN = Pattern.compile("^START_DATE\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern END_DATE_PATTERN = Pattern.compile("^END_DATE\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TIME_RANGE_TYPE_PATTERN = Pattern.compile("^TIME_RANGE_TYPE\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TARGET_DATE_PATTERN = Pattern.compile("^TARGET_DATE\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TARGET_TIME_PATTERN = Pattern.compile("^TARGET_TIME\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TIME_HINT_PATTERN = Pattern.compile("^TIME_HINT\\s*:[ \\t]*([^\\r\\n]*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Set<String> STRUCTURED_EMPTY_VALUES = Set.of("", "N/A", "NONE", "NULL", "UNKNOWN");

    private final ChatMemoryStore chatMemoryStore;
    private final AiBookingSearchTool aiBookingSearchTool;

    public WorkflowBookingIdentificationSupport(
            ChatMemoryStore chatMemoryStore,
            AiBookingSearchTool aiBookingSearchTool
    ) {
        this.chatMemoryStore = chatMemoryStore;
        this.aiBookingSearchTool = aiBookingSearchTool;
    }

    public BookingIdentificationResult identifyBooking(
            Long userId,
            String latestUserMessage,
            String workflowTaskState,
            List<BookingItemVO> actionableBookings,
            List<Long> preferredCandidateBookingIds,
            WorkflowDisambiguationAssistant assistant
    ) {
        List<BookingItemVO> pool = chooseCandidatePool(actionableBookings, preferredCandidateBookingIds);
        if (pool.isEmpty()) {
            return BookingIdentificationResult.needsUserSelection(List.of(), null, null, null, null, null, null, null);
        }
        if (pool.size() == 1) {
            return BookingIdentificationResult.resolved(Long.valueOf(pool.get(0).getId()), pool);
        }

        Long directId = extractDirectBookingId(latestUserMessage, pool);
        if (directId != null) {
            return BookingIdentificationResult.resolved(directId, pool);
        }

        String memoryContext = buildMemoryContext(userId);
        String candidateSummary = buildCandidateSummary(pool);
        String rawAssistantReply = assistant.process(
                Optional.ofNullable(latestUserMessage).orElse(""),
                Optional.ofNullable(workflowTaskState).orElse("identifying booking"),
                memoryContext,
                candidateSummary
        );
        StructuredAssistantReply structuredReply = StructuredAssistantReply.parse(rawAssistantReply);
        if (structuredReply.abort()) {
            return BookingIdentificationResult.aborted(rawAssistantReply);
        }

        if (structuredReply.resolvedBookingId() != null && containsBookingId(pool, structuredReply.resolvedBookingId())) {
            return BookingIdentificationResult.resolved(
                    structuredReply.resolvedBookingId(),
                    pool,
                    structuredReply.startDate(),
                    structuredReply.endDate(),
                    structuredReply.timeRangeType(),
                    structuredReply.targetDate(),
                    structuredReply.targetTime(),
                    structuredReply.timeHint()
            );
        }

        List<BookingItemVO> matchedBookings = resolveFromSearch(userId, pool, structuredReply);
        if (matchedBookings.size() == 1) {
            return BookingIdentificationResult.resolved(
                    Long.valueOf(matchedBookings.get(0).getId()),
                    matchedBookings,
                    structuredReply.startDate(),
                    structuredReply.endDate(),
                    structuredReply.timeRangeType(),
                    structuredReply.targetDate(),
                    structuredReply.targetTime(),
                    structuredReply.timeHint()
            );
        }
        if (!matchedBookings.isEmpty()) {
            return BookingIdentificationResult.needsUserSelection(
                    matchedBookings,
                    structuredReply.summary(),
                    structuredReply.startDate(),
                    structuredReply.endDate(),
                    structuredReply.timeRangeType(),
                    structuredReply.targetDate(),
                    structuredReply.targetTime(),
                    structuredReply.timeHint()
            );
        }
        return BookingIdentificationResult.needsUserSelection(
                pool,
                structuredReply.summary(),
                structuredReply.startDate(),
                structuredReply.endDate(),
                structuredReply.timeRangeType(),
                structuredReply.targetDate(),
                structuredReply.targetTime(),
                structuredReply.timeHint()
        );
    }

    private List<BookingItemVO> resolveFromSearch(
            Long userId,
            List<BookingItemVO> candidatePool,
            StructuredAssistantReply structuredReply
    ) {
        AiBookingSearchDTO searchDTO = structuredReply.toSearchDTO();
        if (searchDTO.getTimeRangeType() == null
                && searchDTO.getStartDate() == null
                && searchDTO.getEndDate() == null) {
            searchDTO.setTimeRangeType("UPCOMING");
        }
        if (isBlank(searchDTO.getExpertName())
                && isBlank(searchDTO.getCategoryName())
                && isBlank(searchDTO.getStatus())
                && searchDTO.getStartDate() == null
                && searchDTO.getEndDate() == null
                && isBlank(searchDTO.getTimeRangeType())) {
            return List.of();
        }

        AiBookingSearchResultVO result = aiBookingSearchTool.searchCurrentCustomerBookings(
                userId,
                searchDTO.getExpertName(),
                searchDTO.getCategoryName(),
                searchDTO.getStatus(),
                searchDTO.getStartDate(),
                searchDTO.getEndDate(),
                searchDTO.getTimeRangeType()
        );
        if (result == null || result.getItems() == null || result.getItems().isEmpty()) {
            return List.of();
        }

        Map<String, BookingItemVO> candidateById = candidatePool.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(BookingItemVO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<BookingItemVO> matched = new ArrayList<>();
        for (AiBookingSearchItemVO item : result.getItems()) {
            if (item == null || item.getBookingId() == null) {
                continue;
            }
            BookingItemVO matchedCandidate = candidateById.get(item.getBookingId());
            if (matchedCandidate != null) {
                matched.add(matchedCandidate);
            }
        }
        return matched.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(BookingItemVO::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private List<BookingItemVO> chooseCandidatePool(List<BookingItemVO> actionableBookings, List<Long> preferredCandidateBookingIds) {
        if (preferredCandidateBookingIds == null || preferredCandidateBookingIds.isEmpty()) {
            return actionableBookings == null ? List.of() : actionableBookings;
        }
        Set<String> preferredIds = preferredCandidateBookingIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<BookingItemVO> filtered = Optional.ofNullable(actionableBookings)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null && preferredIds.contains(item.getId()))
                .toList();
        return filtered.isEmpty() ? Optional.ofNullable(actionableBookings).orElseGet(List::of) : filtered;
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

    private boolean containsBookingId(Collection<BookingItemVO> candidates, Long bookingId) {
        if (bookingId == null) {
            return false;
        }
        String expected = String.valueOf(bookingId);
        return candidates.stream()
                .filter(Objects::nonNull)
                .map(BookingItemVO::getId)
                .anyMatch(expected::equals);
    }

    private String buildMemoryContext(Long userId) {
        List<ChatMessage> messages = chatMemoryStore.getMessages(userId);
        if (messages == null || messages.isEmpty()) {
            return "No recent chat memory.";
        }
        List<String> lines = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message == null || message instanceof SystemMessage) {
                continue;
            }
            if (message instanceof UserMessage userMessage) {
                lines.add("User: " + safeText(userMessage.singleText()));
                continue;
            }
            if (message instanceof AiMessage aiMessage) {
                if (aiMessage.text() != null && !aiMessage.text().isBlank()) {
                    lines.add("Assistant: " + safeText(aiMessage.text()));
                }
                continue;
            }
            if (message instanceof ToolExecutionResultMessage toolResult) {
                String text = toolResult.text();
                if (text != null && !text.isBlank()) {
                    lines.add("Tool " + safeText(toolResult.toolName()) + ": " + safeText(text));
                }
            }
        }
        return lines.isEmpty() ? "No recent chat memory." : String.join("\n", lines);
    }

    private String buildCandidateSummary(List<BookingItemVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "No actionable candidates available.";
        }
        List<String> lines = new ArrayList<>();
        for (BookingItemVO item : candidates) {
            if (item == null) {
                continue;
            }
            lines.add("bookingId=%s, specialist=%s, service=%s, appointment=%s, status=%s".formatted(
                    safeText(item.getId()),
                    safeText(item.getSpecialistName()),
                    safeText(item.getServiceName()),
                    item.getAppointmentDateTime() == null ? "unknown" : item.getAppointmentDateTime().toString(),
                    safeText(item.getStatus())
            ));
        }
        return String.join("\n", lines);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    public interface WorkflowDisambiguationAssistant {
        String process(String userMsg, String taskState, String memoryContext, String candidateSummary);
    }

    public record BookingIdentificationResult(
            Status status,
            Long resolvedBookingId,
            List<BookingItemVO> matchedBookings,
            String message,
            LocalDate lookupStartDate,
            LocalDate lookupEndDate,
            String lookupTimeRangeType,
            String targetDate,
            String targetTime,
            String timeHint
    ) {
        public static BookingIdentificationResult resolved(Long bookingId, List<BookingItemVO> matchedBookings) {
            return new BookingIdentificationResult(Status.RESOLVED, bookingId, matchedBookings, null, null, null, null, null, null, null);
        }

        public static BookingIdentificationResult resolved(
                Long bookingId,
                List<BookingItemVO> matchedBookings,
                LocalDate lookupStartDate,
                LocalDate lookupEndDate,
                String lookupTimeRangeType,
                String targetDate,
                String targetTime,
                String timeHint
        ) {
            return new BookingIdentificationResult(
                    Status.RESOLVED,
                    bookingId,
                    matchedBookings,
                    null,
                    lookupStartDate,
                    lookupEndDate,
                    lookupTimeRangeType,
                    targetDate,
                    targetTime,
                    timeHint
            );
        }

        public static BookingIdentificationResult needsUserSelection(
                List<BookingItemVO> matchedBookings,
                String message,
                LocalDate lookupStartDate,
                LocalDate lookupEndDate,
                String lookupTimeRangeType,
                String targetDate,
                String targetTime,
                String timeHint
        ) {
            return new BookingIdentificationResult(
                    Status.NEEDS_USER_SELECTION,
                    null,
                    matchedBookings,
                    message,
                    lookupStartDate,
                    lookupEndDate,
                    lookupTimeRangeType,
                    targetDate,
                    targetTime,
                    timeHint
            );
        }

        public static BookingIdentificationResult aborted(String message) {
            return new BookingIdentificationResult(Status.ABORTED, null, List.of(), message, null, null, null, null, null, null);
        }
    }

    public enum Status {
        RESOLVED,
        NEEDS_USER_SELECTION,
        ABORTED
    }

    private record StructuredAssistantReply(
            String action,
            Long resolvedBookingId,
            String expertName,
            String categoryName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            String timeRangeType,
            String targetDate,
            String targetTime,
            String timeHint
    ) {
        static StructuredAssistantReply parse(String raw) {
            return new StructuredAssistantReply(
                    normalizedValue(extract(raw, ACTION_PATTERN)),
                    parseLong(extract(raw, BOOKING_ID_PATTERN)),
                    blankToNull(extract(raw, EXPERT_NAME_PATTERN)),
                    blankToNull(extract(raw, CATEGORY_NAME_PATTERN)),
                    blankToNull(extract(raw, STATUS_PATTERN)),
                    parseDate(extract(raw, START_DATE_PATTERN)),
                    parseDate(extract(raw, END_DATE_PATTERN)),
                    blankToNull(extract(raw, TIME_RANGE_TYPE_PATTERN)),
                    normalizeIsoDate(extract(raw, TARGET_DATE_PATTERN)),
                    normalizeTime(extract(raw, TARGET_TIME_PATTERN)),
                    blankToNull(extract(raw, TIME_HINT_PATTERN))
            );
        }

        boolean abort() {
            return "ABORT".equals(action);
        }

        String summary() {
            if ("INSUFFICIENT_INFO".equals(action)) {
                return "I still need a specific booking ID from the matched list.";
            }
            if ("NEEDS_USER_ID_SELECTION".equals(action)) {
                return "Please reply with the exact booking ID from the list below.";
            }
            return null;
        }

        AiBookingSearchDTO toSearchDTO() {
            AiBookingSearchDTO dto = new AiBookingSearchDTO();
            dto.setExpertName(expertName);
            dto.setCategoryName(categoryName);
            dto.setStatus(status == null ? null : status.toUpperCase(Locale.ROOT));
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setTimeRangeType(timeRangeType == null ? null : timeRangeType.toUpperCase(Locale.ROOT));
            return dto;
        }

        private static String extract(String raw, Pattern pattern) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            Matcher matcher = pattern.matcher(raw);
            if (!matcher.find()) {
                return null;
            }
            return matcher.group(1);
        }

        private static String normalizedValue(String raw) {
            String value = blankToNull(raw);
            return value == null ? null : value.toUpperCase(Locale.ROOT);
        }

        private static String blankToNull(String raw) {
            if (raw == null) {
                return null;
            }
            String trimmed = raw.trim();
            if (STRUCTURED_EMPTY_VALUES.contains(trimmed.toUpperCase(Locale.ROOT))) {
                return null;
            }
            return trimmed;
        }

        private static Long parseLong(String raw) {
            String value = blankToNull(raw);
            if (value == null) {
                return null;
            }
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static LocalDate parseDate(String raw) {
            String value = blankToNull(raw);
            if (value == null) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }

        private static String normalizeIsoDate(String raw) {
            String value = blankToNull(raw);
            if (value == null) {
                return null;
            }
            try {
                return LocalDate.parse(value).toString();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }

        private static String normalizeTime(String raw) {
            String value = blankToNull(raw);
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            if (normalized.matches("^\\d{2}:\\d{2}$")) {
                return normalized;
            }
            return null;
        }
    }
}

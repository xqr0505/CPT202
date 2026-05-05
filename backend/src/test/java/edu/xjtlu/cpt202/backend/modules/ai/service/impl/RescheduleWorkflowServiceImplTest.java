package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.RescheduleTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.ai.service.RescheduleWorkflowAssistant;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.AiBookingSearchService;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
/**
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
class RescheduleWorkflowServiceImplTest {

    @Test
    void shouldStartWorkflowForEnglishRescheduleIntent() {
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                new InMemoryRescheduleTaskStateStore(),
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "I want to reschedule my booking");

        assertThat(shouldStart).isTrue();
    }

    @Test
    void shouldNotStartWorkflowForReschedulePolicyQuestion() {
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                new InMemoryRescheduleTaskStateStore(),
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.KNOWLEDGE,
                support()
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "what is the reschedule policy");

        assertThat(shouldStart).isFalse();
    }

    @Test
    void shouldNotStartWorkflowForRescheduleEligibilityQuestion() {
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                new InMemoryRescheduleTaskStateStore(),
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.KNOWLEDGE,
                support()
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "Can I reschedule to another specialist?");

        assertThat(shouldStart).isFalse();
    }

    @Test
    void shouldStartWorkflowForMisspelledRescheduleIntentWithToDatePhrase() {
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                new InMemoryRescheduleTaskStateStore(),
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.DASHBOARD,
                support()
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "reschedul my booking with S1 to 5/13");

        assertThat(shouldStart).isTrue();
    }

    @Test
    void shouldStartWorkflowForChineseRescheduleIntent() {
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                new InMemoryRescheduleTaskStateStore(),
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        boolean shouldStart = service.shouldStartWorkflow(1001L, "我要改期");

        assertThat(shouldStart).isTrue();
    }

    @Test
    void shouldReturnMarkdownTableWhenBookingIsAmbiguous() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED"),
                booking("15", "Dr. Victoria Jiang", "Urgent Review", LocalDateTime.of(2026, 5, 19, 21, 0), "CONFIRMED")
        );
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("I want to reschedule my booking"));

        assertThat(reply).contains("Please reply with the exact booking ID");
        assertThat(reply).contains("| Booking ID | Specialist | Service | Appointment Time | Status |");
        assertThat(reply).contains("| 14 | Dr. Olivia Wang | Evening Consult | 2026-05-12 21:00 | CONFIRMED |");
        assertThat(store.get(1001L)).isPresent();
        assertThat(store.get(1001L).get().getStep()).isEqualTo(RescheduleTaskState.Step.IDENTIFY);
    }

    @Test
    void shouldDefaultTargetDateToOriginalAppointmentDate() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .specialistName("Dr. Olivia Wang")
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .price(new BigDecimal("200.00"))
                .topic("Evening Consult")
                .build();
        specialistQueryService.availability = List.of(slot(301L, "2026-05-12", "16:00", "AVAILABLE"));
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14"));

        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-12:]");
        assertThat(bookingService.lastRescheduleQuoteBookingId).isNull();
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldExtractExplicitTargetDate() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .specialistName("Dr. Olivia Wang")
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        specialistQueryService.availability = List.of(slot(301L, "2026-05-19", "16:00", "AVAILABLE"));
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-19"));

        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-19:]");
    }

    @Test
    void shouldPreselectSlotWhenTimeIntentIsClear() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .specialistName("Dr. Olivia Wang")
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        bookingService.rescheduleQuote = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .message("Allowed")
                .build();
        specialistQueryService.availability = List.of(
                slot(301L, "2026-05-16", "09:00", "AVAILABLE"),
                slot(302L, "2026-05-16", "15:00", "AVAILABLE")
        );
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-16 around 3pm"));

        assertThat(bookingService.lastRescheduleQuoteBookingId).isEqualTo(14L);
        assertThat(bookingService.lastRescheduleQuoteSlotId).isEqualTo(302L);
        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-16:302]");
    }

    @Test
    void shouldNotPreselectSlotForDateOnlyRequest() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        specialistQueryService.availability = List.of(
                slot(301L, "2026-05-16", "09:00", "AVAILABLE"),
                slot(302L, "2026-05-16", "15:00", "AVAILABLE")
        );
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-16"));

        assertThat(bookingService.lastRescheduleQuoteSlotId).isNull();
        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-16:]");
    }

    @Test
    void shouldReturnNoAvailabilityMessageAndStillTriggerDateOnlyModal() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        specialistQueryService.availability = List.of();
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-16"));

        assertThat(reply).contains("has no available time slots on 2026-05-16");
        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-16:]");
    }

    @Test
    void shouldClearRedisOnAbortPhrases() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        store.save(1001L, RescheduleTaskState.builder().step(RescheduleTaskState.Step.IDENTIFY).build());
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                new StubBookingService(),
                new StubSpecialistQueryService(),
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("never mind"));

        assertThat(reply).startsWith("[RESCHEDULE_TASK_ABORTED]");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldClearRedisOnValidationFailureAndReturnQuoteReason() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        bookingService.rescheduleQuote = BookingRescheduleQuoteVO.builder()
                .allowed(false)
                .message("Reschedule is not allowed within 2 hours.")
                .build();
        specialistQueryService.availability = List.of(slot(302L, "2026-05-16", "15:00", "AVAILABLE"));
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-16 around 3pm"));

        assertThat(reply).contains("Reschedule is not allowed within 2 hours.");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldEmitTriggerMarkerWhenValidationSucceeds() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        bookingService.rescheduleQuote = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .build();
        specialistQueryService.availability = List.of(slot(302L, "2026-05-16", "15:00", "AVAILABLE"));
        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                assistantNeedsUserSelection(),
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("reschedule booking 14 to 2026-05-16 around 3pm"));

        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-16:302]");
        assertThat(store.get(1001L)).isEmpty();
    }

    @Test
    void shouldResolveBookingFromAssistantProvidedIdWhenFollowUpIsVague() {
        InMemoryRescheduleTaskStateStore store = new InMemoryRescheduleTaskStateStore();
        StubBookingService bookingService = new StubBookingService();
        StubSpecialistQueryService specialistQueryService = new StubSpecialistQueryService();
        bookingService.bookingList = List.of(
                booking("14", "Dr. Olivia Wang", "Evening Consult", LocalDateTime.of(2026, 5, 12, 21, 0), "CONFIRMED")
        );
        bookingService.bookingDetail = BookingDetailVO.builder()
                .bookingId(14L)
                .specialistId(101L)
                .slotDate("2026-05-12")
                .startTime("21:00")
                .status("CONFIRMED")
                .build();
        specialistQueryService.availability = List.of(slot(302L, "2026-05-16", "15:00", "AVAILABLE"));

        RescheduleWorkflowServiceImpl service = new RescheduleWorkflowServiceImpl(
                store,
                (userMsg, taskState, memoryContext, candidateSummary) -> """
                        ACTION: RESOLVED_BOOKING_ID
                        BOOKING_ID: 14
                        EXPERT_NAME: N/A
                        CATEGORY_NAME: N/A
                        STATUS: N/A
                        START_DATE: N/A
                        END_DATE: N/A
                        TIME_RANGE_TYPE: N/A
                        """,
                bookingService,
                specialistQueryService,
                (memoryId, userMessage) -> AiIntent.BOOKING,
                support()
        );

        String reply = service.handle(1001L, wrapped("that one"));

        assertThat(reply).contains("[TRIGGER_RESCHEDULE_MODAL:14:2026-05-12:]");
    }

    private static BookingItemVO booking(String id, String specialist, String service, LocalDateTime time, String status) {
        return BookingItemVO.builder()
                .id(id)
                .specialistName(specialist)
                .serviceName(service)
                .appointmentDateTime(time)
                .status(status)
                .build();
    }

    private static SpecialistAvailabilityVO slot(Long id, String date, String startTime, String status) {
        SpecialistAvailabilityVO slot = new SpecialistAvailabilityVO();
        slot.setId(id);
        slot.setSlotDate(LocalDate.parse(date));
        slot.setStartTime(LocalTime.parse(startTime));
        slot.setEndTime(LocalTime.parse(startTime).plusMinutes(30));
        slot.setStatus(status);
        return slot;
    }

    private static String wrapped(String userMessage) {
        return """
                Current system time: 2026-05-04 10:00:00 CST
                Use this as the authoritative current time when interpreting relative dates such as today, tomorrow, upcoming, this week, and history.

                User message:
                %s
                """.formatted(userMessage);
    }

    private static RescheduleWorkflowAssistant assistantNeedsUserSelection() {
        return (userMsg, taskState, memoryContext, candidateSummary) -> """
                ACTION: NEEDS_USER_ID_SELECTION
                BOOKING_ID: N/A
                EXPERT_NAME: N/A
                CATEGORY_NAME: N/A
                STATUS: N/A
                START_DATE: N/A
                END_DATE: N/A
                TIME_RANGE_TYPE: N/A
                """;
    }

    private static WorkflowBookingIdentificationSupport support() {
        StubAiBookingSearchService searchService = new StubAiBookingSearchService();
        return new WorkflowBookingIdentificationSupport(
                new InMemoryChatMemoryStore(),
                new edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool(searchService)
        );
    }

    private static class InMemoryRescheduleTaskStateStore implements RescheduleTaskStateStore {

        private final Map<Long, RescheduleTaskState> states = new HashMap<>();

        @Override
        public Optional<RescheduleTaskState> get(Long userId) {
            return Optional.ofNullable(states.get(userId));
        }

        @Override
        public void save(Long userId, RescheduleTaskState state) {
            states.put(userId, state);
        }

        @Override
        public void clear(Long userId) {
            states.remove(userId);
        }
    }

    private static class InMemoryChatMemoryStore implements ChatMemoryStore {
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return List.of();
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        }

        @Override
        public void deleteMessages(Object memoryId) {
        }
    }

    private static class StubAiBookingSearchService implements AiBookingSearchService {
        @Override
        public AiBookingSearchResultVO searchCustomerBookings(Long customerId, AiBookingSearchDTO queryDTO) {
            return AiBookingSearchResultVO.builder()
                    .totalMatched(0)
                    .returnedCount(0)
                    .items(List.of())
                    .build();
        }
    }

    private static class StubSpecialistQueryService implements SpecialistQueryService {

        private List<SpecialistAvailabilityVO> availability = List.of();

        @Override
        public List<SpecialistCategoryVO> listCategories() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<SpecialistSummaryVO> searchSpecialists(SpecialistSearchQueryDTO query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpecialistDetailVO getSpecialistDetail(Long specialistId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpecialistAvailabilityVO> listAvailability(Long specialistId, LocalDate date) {
            return availability.stream()
                    .filter(slot -> Objects.equals(slot.getSlotDate(), date))
                    .toList();
        }
    }

    private static class StubBookingService implements BookingService {

        private List<BookingItemVO> bookingList = List.of();
        private BookingDetailVO bookingDetail;
        private BookingRescheduleQuoteVO rescheduleQuote = BookingRescheduleQuoteVO.builder().allowed(true).build();
        private Long lastRescheduleQuoteBookingId;
        private Long lastRescheduleQuoteSlotId;

        @Override
        public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int autoCompleteExpiredConfirmedBookings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int autoCancelExpiredPendingBookings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingCreateVO createBooking(Long customerId, BookingCreateDTO createDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiBookingFormDraftVO buildAiBookingDraft(Long customerId, Long specialistId, Long slotId, String preferredTopic, String customerNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto) {
            return new PageResult<>(bookingList.size(), bookingList);
        }

        @Override
        public UsageSummaryVO getUsageSummary(UsageSummaryQueryDTO queryDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DashboardStatisticsVO getDashboardStatistics(DashboardQueryDTO queryDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingDetailVO getBookingDetailById(Long bookingId, Long currentCustomerId) {
            return bookingDetail;
        }

        @Override
        public BookingCancelQuoteVO customerCancellationQuote(Long bookingId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingCancelConfirmVO customerCancellationConfirm(Long bookingId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingRescheduleQuoteVO customerRescheduleQuote(Long bookingId, Long newSlotId, Long currentCustomerId) {
            lastRescheduleQuoteBookingId = bookingId;
            lastRescheduleQuoteSlotId = newSlotId;
            if (rescheduleQuote != null && !rescheduleQuote.isAllowed()) {
                throw new BusinessException(400, rescheduleQuote.getMessage());
            }
            return rescheduleQuote;
        }

        @Override
        public BookingRescheduleConfirmVO customerRescheduleConfirm(Long bookingId, Long newSlotId, Long currentCustomerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpecialistPendingBookingVO> listPendingRequestsForSpecialist(Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpecialistHandledBookingVO> listHandledRequestsForSpecialist(Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpecialistBookingDetailVO getBookingRequestDetailForSpecialist(Long bookingId, Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void approveBookingRequest(Long bookingId, Long currentUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rejectBookingRequest(Long bookingId, Long currentUserId, SpecialistRejectBookingRequestDTO requestDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void specialistForceCancelBooking(Long bookingId, Long currentUserId, SpecialistForceCancelBookingRequestDTO requestDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void systemTimeoutCancelPendingBooking(Long bookingId, String cancelReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean save(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveBatch(Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdateBatch(Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateBatchById(Collection<Booking> entityList, int batchSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdate(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Booking getById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Booking> listByIds(Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Booking> listByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Booking getOne(Wrapper<Booking> queryWrapper, boolean throwEx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Booking> getOneOpt(Wrapper<Booking> queryWrapper, boolean throwEx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> getMap(Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> V getObj(Wrapper<Booking> queryWrapper, Function<? super Object, V> mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count(Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Booking> list(Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Booking> list() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E extends IPage<Booking>> E page(E page, Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E extends IPage<Booking>> E page(E page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> listMaps(Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> listMaps() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> List<V> listObjs(Wrapper<Booking> queryWrapper, Function<? super Object, V> mapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object> listObjs() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Wrapper<Booking> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeByIds(Collection<?> list) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateById(Booking entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update(Wrapper<Booking> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean update(Booking entity, Wrapper<Booking> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveBatch(Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean saveOrUpdateBatch(Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateBatchById(Collection<Booking> entityList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BaseMapper<Booking> getBaseMapper() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<Booking> getEntityClass() {
            return Booking.class;
        }
    }
}

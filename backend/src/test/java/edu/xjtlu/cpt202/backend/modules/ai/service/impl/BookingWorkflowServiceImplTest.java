package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.ai.model.BookingTaskState;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntent;
import edu.xjtlu.cpt202.backend.modules.ai.service.AiIntentRouterService;
import edu.xjtlu.cpt202.backend.modules.ai.service.BookingTaskStateStore;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author QiranXiao
 * @since 2026/5/5
 */
class BookingWorkflowServiceImplTest {

    @Test
    void shouldStartWorkflowForBookingOrderOrAvailabilityLookup() {
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        AiIntentRouterService aiIntentRouterService = mock(AiIntentRouterService.class);
        when(aiIntentRouterService.resolveIntent(anyLong(), any())).thenReturn(AiIntent.KNOWLEDGE, AiIntent.BOOKING);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                new InMemoryBookingTaskStateStore(),
                specialistQueryService,
                bookingService,
                aiIntentRouterService
        );

        boolean availabilityQuery = service.shouldStartWorkflow(1001L, "show me specialist availability tomorrow");
        boolean explicitOrder = service.shouldStartWorkflow(1001L, "I want to place booking order now");

        assertThat(availabilityQuery).isTrue();
        assertThat(explicitOrder).isTrue();
    }

    @Test
    void shouldNotStartWorkflowForPureKnowledgeQuestion() {
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        AiIntentRouterService aiIntentRouterService = mock(AiIntentRouterService.class);
        when(aiIntentRouterService.resolveIntent(anyLong(), any())).thenReturn(AiIntent.KNOWLEDGE);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                new InMemoryBookingTaskStateStore(),
                specialistQueryService,
                bookingService,
                aiIntentRouterService
        );

        boolean knowledgeQuery = service.shouldStartWorkflow(1001L, "what is cancellation policy");

        assertThat(knowledgeQuery).isFalse();
    }

    @Test
    void shouldStartWorkflowForChineseLookupBySpecialist() {
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        AiIntentRouterService aiIntentRouterService = mock(AiIntentRouterService.class);
        when(aiIntentRouterService.resolveIntent(anyLong(), any())).thenReturn(AiIntent.KNOWLEDGE);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                new InMemoryBookingTaskStateStore(),
                specialistQueryService,
                bookingService,
                aiIntentRouterService
        );

        boolean lookupQuery = service.shouldStartWorkflow(1001L, "\u5e2e\u6211\u67e5\u8be2lic\u4e13\u5bb6\u53ef\u7528\u65f6\u95f4");

        assertThat(lookupQuery).isTrue();
    }

    @Test
    void shouldReturnSlotTableWhenUserProvidesSpecialistNameAndDate() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                bookingService,
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        mockSpecialistAndSlots(specialistQueryService);

        String reply = service.handle(1001L, wrapped("Book Dr. Emily Chen on 2026-05-20"));

        assertThat(reply).contains("| Specialist ID | Specialist | Date | Slot ID | Start | End | Fee |");
        assertThat(reply).contains("| 101 | Dr. Emily Chen | 2026-05-20 | 9001 | 10:00 | 10:30 | 200.00 |");
        assertThat(reply).contains("| 101 | Dr. Emily Chen | 2026-05-20 | 9002 | 11:00 | 11:30 | 200.00 |");
        assertThat(store.get(1001L)).isPresent();
        assertThat(store.get(1001L).get().getCandidateSlots()).hasSize(2);
        verify(bookingService, never()).buildAiBookingDraft(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void shouldReturnNearestThreeDaySlotsWhenUserProvidesOnlySpecialist() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        LocalDate today = LocalDate.now();
        mockSpecialistsForDate(specialistQueryService, today, List.of(slot(9101L, today, "10:00", "10:30")));
        mockSpecialistsForDate(specialistQueryService, today.plusDays(1), List.of(slot(9102L, today.plusDays(1), "09:00", "09:30")));
        mockSpecialistsForDate(specialistQueryService, today.plusDays(2), List.of(slot(9103L, today.plusDays(2), "11:00", "11:30")));

        String reply = service.handle(1001L, wrapped("Book Dr. Emily Chen"));

        assertThat(reply).contains("next 3 days");
        assertThat(reply).contains(today.toString());
        assertThat(reply).contains(today.plusDays(1).toString());
        assertThat(reply).contains(today.plusDays(2).toString());
        assertThat(reply).contains("| 101 | Dr. Emily Chen |");
        assertThat(store.get(1001L)).isPresent();
        assertThat(store.get(1001L).get().getCandidateSlots()).hasSize(3);
    }

    @Test
    void shouldParseSpecialistNameWhenNameIsBeforeChineseExpertKeyword() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        LocalDate today = LocalDate.now();

        SpecialistSummaryVO specialistSummary = new SpecialistSummaryVO();
        specialistSummary.setId(101L);
        specialistSummary.setName("Lic");
        specialistSummary.setConsultationFee(new BigDecimal("200.00"));
        when(specialistQueryService.searchSpecialists(any())).thenAnswer(invocation -> {
            SpecialistSearchQueryDTO queryDTO = invocation.getArgument(0);
            if (queryDTO != null && "lic".equalsIgnoreCase(String.valueOf(queryDTO.getKeyword()))) {
                return new PageResult<>(1L, List.of(specialistSummary));
            }
            return new PageResult<>(0L, List.of());
        });

        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(101L);
        detail.setName("Lic");
        detail.setConsultationFee(new BigDecimal("200.00"));
        detail.setStatus("ACTIVE");
        when(specialistQueryService.getSpecialistDetail(101L)).thenReturn(detail);
        when(specialistQueryService.listAvailability(eq(101L), any(LocalDate.class))).thenReturn(List.of(
                slot(9401L, today, "10:00", "10:30")
        ));

        String reply = service.handle(1001L, wrapped("\u5e2e\u6211\u770b\u770blic\u4e13\u5bb6\u53ef\u7528\u65f6\u95f4\u6bb5\uff0c\u6211\u8981\u9884\u5b9a"));

        assertThat(reply).contains("| Specialist ID | Specialist | Date | Slot ID | Start | End | Fee |");
        assertThat(reply).contains("| 101 | Lic |");
        assertThat(reply).doesNotContain("I could not find available slots for this specialist in the next 3 days");
    }

    @Test
    void shouldFindSuitableSpecialistsByDateAndMorningPreference() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        LocalDate date = LocalDate.parse("2026-05-20");

        SpecialistSummaryVO first = new SpecialistSummaryVO();
        first.setId(101L);
        first.setName("Dr. Emily Chen");
        first.setConsultationFee(new BigDecimal("200.00"));
        SpecialistSummaryVO second = new SpecialistSummaryVO();
        second.setId(102L);
        second.setName("Dr. Daniel Wu");
        second.setConsultationFee(new BigDecimal("180.00"));
        when(specialistQueryService.searchSpecialists(any())).thenReturn(new PageResult<>(2L, List.of(first, second)));

        SpecialistDetailVO detailOne = new SpecialistDetailVO();
        detailOne.setId(101L);
        detailOne.setName("Dr. Emily Chen");
        detailOne.setConsultationFee(new BigDecimal("200.00"));
        detailOne.setStatus("ACTIVE");
        when(specialistQueryService.getSpecialistDetail(101L)).thenReturn(detailOne);

        SpecialistDetailVO detailTwo = new SpecialistDetailVO();
        detailTwo.setId(102L);
        detailTwo.setName("Dr. Daniel Wu");
        detailTwo.setConsultationFee(new BigDecimal("180.00"));
        detailTwo.setStatus("ACTIVE");
        when(specialistQueryService.getSpecialistDetail(102L)).thenReturn(detailTwo);

        when(specialistQueryService.listAvailability(101L, date)).thenReturn(List.of(
                slot(9201L, date, "09:30", "10:00"),
                slot(9202L, date, "15:00", "15:30")
        ));
        when(specialistQueryService.listAvailability(102L, date)).thenReturn(List.of(
                slot(9301L, date, "10:30", "11:00")
        ));

        String reply = service.handle(1001L, wrapped("I want to book 2026-05-20 morning"));

        assertThat(reply).contains("| 101 | Dr. Emily Chen | 2026-05-20 | 9201 | 09:30 | 10:00 | 200.00 |");
        assertThat(reply).contains("| 102 | Dr. Daniel Wu | 2026-05-20 | 9301 | 10:30 | 11:00 | 180.00 |");
        assertThat(reply).doesNotContain("| 101 | Dr. Emily Chen | 2026-05-20 | 9202 | 15:00 | 15:30 |");
        assertThat(store.get(1001L)).isPresent();
        assertThat(store.get(1001L).get().getCandidateSlots()).hasSize(2);
    }



    @Test
    void shouldReturnOnlyDawnSlotsWhenUserRequestsDawnPeriod() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        LocalDate date = LocalDate.parse("2026-05-21");

        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(101L);
        specialist.setName("Lic");
        specialist.setConsultationFee(new BigDecimal("200.00"));
        when(specialistQueryService.searchSpecialists(any())).thenReturn(new PageResult<>(1L, List.of(specialist)));

        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(101L);
        detail.setName("Lic");
        detail.setConsultationFee(new BigDecimal("200.00"));
        detail.setStatus("ACTIVE");
        when(specialistQueryService.getSpecialistDetail(101L)).thenReturn(detail);
        when(specialistQueryService.listAvailability(101L, date)).thenReturn(List.of(
                slot(9701L, date, "03:30", "04:00"),
                slot(9702L, date, "09:00", "09:30")
        ));

        String reply = service.handle(1001L, wrapped("\u6211\u8981\u9884\u7ea65\u670821\u53f7\u51cc\u6668\u65f6\u95f4"));

        assertThat(reply).contains("| 101 | Lic | 2026-05-21 | 9701 | 03:30 | 04:00 | 200.00 |");
        assertThat(reply).doesNotContain("| 101 | Lic | 2026-05-21 | 9702 | 09:00 | 09:30 |");
    }



    @Test
    void shouldReturnBookingPreviewMarkerAfterUserPicksSlotId() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        SpecialistQueryService specialistQueryService = mock(SpecialistQueryService.class);
        BookingService bookingService = mock(BookingService.class);
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                specialistQueryService,
                bookingService,
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        mockSpecialistAndSlots(specialistQueryService);
        when(bookingService.buildAiBookingDraft(eq(1001L), eq(101L), eq(9001L), eq("Initial Consultation"), eq("Bring reports")))
                .thenReturn(AiBookingFormDraftVO.builder()
                        .customerId(1001L)
                        .specialistId(101L)
                        .slotId(9001L)
                        .topic("Initial Consultation")
                        .customerNotes("Bring reports")
                        .availableTopics(List.of("Initial Consultation", "Follow-up"))
                        .warnings(List.of())
                        .build());

        service.handle(1001L, wrapped("""
                Book Dr. Emily Chen on 2026-05-20
                topic: Initial Consultation
                notes: Bring reports
                """));
        String reply = service.handle(1001L, wrapped("9001"));

        assertThat(reply).contains(BookingWorkflowServiceImpl.BOOKING_PREVIEW_MARKER);
        assertThat(reply).contains("\"slotId\":9001");
        assertThat(reply).contains("\"specialistId\":101");
        assertThat(reply).contains("\"availableTopics\":[\"Initial Consultation\",\"Follow-up\"]");
        assertThat(store.get(1001L)).isEmpty();
        verify(bookingService).buildAiBookingDraft(1001L, 101L, 9001L, "Initial Consultation", "Bring reports");
    }

    @Test
    void shouldKeepWorkflowActiveUntilExplicitLeaveRequest() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                mock(SpecialistQueryService.class),
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        store.save(1001L, BookingTaskState.builder()
                .taskStateText("waiting for slot")
                .candidateSlots(List.of(BookingTaskState.CandidateSlotState.builder()
                        .specialistId(101L)
                        .specialistName("Dr. Emily Chen")
                        .consultationFee(new BigDecimal("200.00"))
                        .slotId(9001L)
                        .slotDate("2026-05-20")
                        .startTime("10:00:00")
                        .endTime("10:30:00")
                        .build()))
                .build());

        String continueReply = service.handle(1001L, wrapped("what does this fee include?"));
        assertThat(store.get(1001L)).isPresent();
        String leaveReply = service.handle(1001L, wrapped("leave booking"));

        assertThat(continueReply).contains("Reply with Slot ID only");
        assertThat(store.get(1001L)).isEmpty();
        assertThat(leaveReply).startsWith(BookingWorkflowServiceImpl.BOOKING_TASK_ABORTED_MARKER);
    }

    @Test
    void shouldExitWorkflowWhenUserSaysNoLongerWantToBookInChinese() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                mock(SpecialistQueryService.class),
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        store.save(1001L, BookingTaskState.builder()
                .taskStateText("waiting for slot")
                .candidateSlots(List.of(BookingTaskState.CandidateSlotState.builder()
                        .specialistId(32L)
                        .specialistName("lic")
                        .consultationFee(new BigDecimal("165.00"))
                        .slotId(1733L)
                        .slotDate("2026-05-05")
                        .startTime("11:53:22")
                        .endTime("13:53:22")
                        .build()))
                .build());

        String leaveReply = service.handle(1001L, wrapped("\u6211\u4e0d\u60f3\u9884\u5b9a\u4e86"));

        assertThat(store.get(1001L)).isEmpty();
        assertThat(leaveReply).startsWith(BookingWorkflowServiceImpl.BOOKING_TASK_ABORTED_MARKER);
        assertThat(leaveReply).doesNotContain("| Specialist ID | Specialist | Date | Slot ID | Start | End | Fee |");
    }

    @Test
    void shouldAbortBookingWorkflowWhenUserSendsGreetingOnly() {
        InMemoryBookingTaskStateStore store = new InMemoryBookingTaskStateStore();
        BookingWorkflowServiceImpl service = new BookingWorkflowServiceImpl(
                store,
                mock(SpecialistQueryService.class),
                mock(BookingService.class),
                (memoryId, userMessage) -> AiIntent.BOOKING
        );
        store.save(1001L, BookingTaskState.builder()
                .taskStateText("waiting for slot")
                .candidateSlots(List.of(BookingTaskState.CandidateSlotState.builder()
                        .specialistId(34L)
                        .specialistName("Melody Coc")
                        .consultationFee(new BigDecimal("215.00"))
                        .slotId(1769L)
                        .slotDate("2026-05-06")
                        .startTime("01:43:37")
                        .endTime("02:43:38")
                        .build()))
                .build());

        String reply = service.handle(1001L, wrapped("\u4f60\u597d"));

        assertThat(store.get(1001L)).isEmpty();
        assertThat(reply).startsWith(BookingWorkflowServiceImpl.BOOKING_TASK_ABORTED_MARKER);
    }

    private static void mockSpecialistAndSlots(SpecialistQueryService specialistQueryService) {
        mockSpecialistsForDate(specialistQueryService, LocalDate.parse("2026-05-20"), List.of(
                slot(9001L, LocalDate.parse("2026-05-20"), "10:00", "10:30"),
                slot(9002L, LocalDate.parse("2026-05-20"), "11:00", "11:30")
        ));
    }

    private static void mockSpecialistsForDate(
            SpecialistQueryService specialistQueryService,
            LocalDate date,
            List<SpecialistAvailabilityVO> slots
    ) {
        SpecialistSummaryVO specialistSummary = new SpecialistSummaryVO();
        specialistSummary.setId(101L);
        specialistSummary.setName("Dr. Emily Chen");
        specialistSummary.setConsultationFee(new BigDecimal("200.00"));
        when(specialistQueryService.searchSpecialists(any()))
                .thenReturn(new PageResult<>(1L, List.of(specialistSummary)));

        SpecialistDetailVO specialistDetail = new SpecialistDetailVO();
        specialistDetail.setId(101L);
        specialistDetail.setName("Dr. Emily Chen");
        specialistDetail.setConsultationFee(new BigDecimal("200.00"));
        specialistDetail.setStatus("ACTIVE");
        when(specialistQueryService.getSpecialistDetail(101L)).thenReturn(specialistDetail);

        when(specialistQueryService.listAvailability(101L, date))
                .thenReturn(slots);
    }

    private static SpecialistAvailabilityVO slot(Long id, LocalDate date, String start, String end) {
        SpecialistAvailabilityVO slot = new SpecialistAvailabilityVO();
        slot.setId(id);
        slot.setSlotDate(date);
        slot.setStartTime(LocalTime.parse(start + ":00"));
        slot.setEndTime(LocalTime.parse(end + ":00"));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        return slot;
    }

    private static String wrapped(String userMessage) {
        return """
                Current system time: 2026-05-05 10:00:00 CST
                Use this as the authoritative current time when interpreting relative dates such as today, tomorrow, upcoming, this week, and history.

                User message:
                %s
                """.formatted(userMessage);
    }

    private static class InMemoryBookingTaskStateStore implements BookingTaskStateStore {

        private final Map<Long, BookingTaskState> states = new HashMap<>();

        @Override
        public Optional<BookingTaskState> get(Long userId) {
            return Optional.ofNullable(states.get(userId));
        }

        @Override
        public void save(Long userId, BookingTaskState state) {
            states.put(userId, state);
        }

        @Override
        public void clear(Long userId) {
            states.remove(userId);
        }
    }
}

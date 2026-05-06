package edu.xjtlu.cpt202.backend.modules.ai.service.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.xjtlu.cpt202.backend.modules.ai.tool.AiBookingSearchTool;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.AiBookingSearchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowBookingIdentificationSupportTest {

    @Test
    void shouldResolveUniqueBookingFromAssistantSearchHints() {
        WorkflowBookingIdentificationSupport support = new WorkflowBookingIdentificationSupport(
                new StubChatMemoryStore(),
                new AiBookingSearchTool(new StubAiBookingSearchService())
        );

        List<BookingItemVO> candidates = List.of(
                booking("21", "Dr. Smith", "Therapy", "CONFIRMED"),
                booking("22", "Dr. Lee", "Consultation", "PENDING")
        );

        WorkflowBookingIdentificationSupport.BookingIdentificationResult result = support.identifyBooking(
                1001L,
                "that one",
                "identifying booking to cancel",
                candidates,
                candidates.stream().map(BookingItemVO::getId).map(Long::valueOf).toList(),
                (userMsg, taskState, memoryContext, candidateSummary) -> """
                        ACTION: NEEDS_USER_ID_SELECTION
                        BOOKING_ID: N/A
                        EXPERT_NAME: Smith
                        CATEGORY_NAME: N/A
                        STATUS: CONFIRMED
                        START_DATE: N/A
                        END_DATE: N/A
                        TIME_RANGE_TYPE: UPCOMING
                        """
        );

        assertThat(result.status()).isEqualTo(WorkflowBookingIdentificationSupport.Status.RESOLVED);
        assertThat(result.resolvedBookingId()).isEqualTo(21L);
    }

    @Test
    void shouldTreatBlankStructuredFieldsAsEmptyInsteadOfShiftingIntoNextLabels() {
        RecordingAiBookingSearchService searchService = new RecordingAiBookingSearchService();
        WorkflowBookingIdentificationSupport support = new WorkflowBookingIdentificationSupport(
                new StubChatMemoryStore(),
                new AiBookingSearchTool(searchService)
        );

        List<BookingItemVO> candidates = List.of(
                booking("21", "Dr. Smith", "Therapy", "CONFIRMED"),
                booking("22", "Dr. Lee", "Consultation", "PENDING")
        );

        WorkflowBookingIdentificationSupport.BookingIdentificationResult result = support.identifyBooking(
                1001L,
                "cancel the booking tomorrow",
                "identifying booking to cancel",
                candidates,
                candidates.stream().map(BookingItemVO::getId).map(Long::valueOf).toList(),
                (userMsg, taskState, memoryContext, candidateSummary) -> """
                        ACTION:
                        BOOKING_ID:
                        EXPERT_NAME:
                        CATEGORY_NAME:
                        STATUS:
                        START_DATE:
                        END_DATE:
                        TIME_RANGE_TYPE:
                        """
        );

        assertThat(result.status()).isEqualTo(WorkflowBookingIdentificationSupport.Status.RESOLVED);
        assertThat(result.resolvedBookingId()).isEqualTo(21L);
        assertThat(searchService.lastQueryDTO).isNotNull();
        assertThat(searchService.lastQueryDTO.getExpertName()).isNull();
        assertThat(searchService.lastQueryDTO.getCategoryName()).isNull();
        assertThat(searchService.lastQueryDTO.getStatus()).isNull();
        assertThat(searchService.lastQueryDTO.getStartDate()).isNull();
        assertThat(searchService.lastQueryDTO.getEndDate()).isNull();
        assertThat(searchService.lastQueryDTO.getTimeRangeType()).isEqualTo("UPCOMING");
    }

    @Test
    void shouldKeepLookupTimeAndTargetTimeSeparated() {
        RecordingAiBookingSearchService searchService = new RecordingAiBookingSearchService();
        WorkflowBookingIdentificationSupport support = new WorkflowBookingIdentificationSupport(
                new StubChatMemoryStore(),
                new AiBookingSearchTool(searchService)
        );

        List<BookingItemVO> candidates = List.of(
                booking("21", "Dr. Smith", "Therapy", "CONFIRMED"),
                booking("22", "Dr. Lee", "Consultation", "PENDING")
        );

        WorkflowBookingIdentificationSupport.BookingIdentificationResult result = support.identifyBooking(
                1001L,
                "I want to move tomorrow's booking to next Friday",
                "identifying booking to reschedule",
                candidates,
                candidates.stream().map(BookingItemVO::getId).map(Long::valueOf).toList(),
                (userMsg, taskState, memoryContext, candidateSummary) -> """
                        ACTION: NEEDS_USER_ID_SELECTION
                        BOOKING_ID: N/A
                        EXPERT_NAME: N/A
                        CATEGORY_NAME: N/A
                        STATUS: N/A
                        START_DATE: 2026-05-07
                        END_DATE: 2026-05-07
                        TIME_RANGE_TYPE: N/A
                        TARGET_DATE: 2026-05-09
                        TARGET_TIME: N/A
                        TIME_HINT: N/A
                        """
        );

        assertThat(result.lookupStartDate()).isEqualTo(java.time.LocalDate.parse("2026-05-07"));
        assertThat(result.lookupEndDate()).isEqualTo(java.time.LocalDate.parse("2026-05-07"));
        assertThat(result.targetDate()).isEqualTo("2026-05-09");
    }

    private static BookingItemVO booking(String id, String specialist, String service, String status) {
        return BookingItemVO.builder()
                .id(id)
                .specialistName(specialist)
                .serviceName(service)
                .status(status)
                .appointmentDateTime(LocalDateTime.of(2026, 5, 10, 9, 0))
                .build();
    }

    private static class StubChatMemoryStore implements ChatMemoryStore {
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return List.of(UserMessage.userMessage("I want to cancel my upcoming booking with Dr. Smith"));
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
                    .totalMatched(1)
                    .returnedCount(1)
                    .items(List.of(AiBookingSearchItemVO.builder()
                            .bookingId("21")
                            .specialistName("Dr. Smith")
                            .status("CONFIRMED")
                            .build()))
                    .build();
        }
    }

    private static class RecordingAiBookingSearchService extends StubAiBookingSearchService {

        private AiBookingSearchDTO lastQueryDTO;

        @Override
        public AiBookingSearchResultVO searchCustomerBookings(Long customerId, AiBookingSearchDTO queryDTO) {
            this.lastQueryDTO = queryDTO;
            return super.searchCustomerBookings(customerId, queryDTO);
        }
    }
}

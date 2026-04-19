package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingTopicMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.CustomerBookingChangePolicyService;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplAiDraftTest {

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingTopicMapper bookingTopicMapper;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistQueryService specialistQueryService;

    @Mock
    private CustomerBookingChangePolicyService customerBookingChangePolicyService;

    @Mock
    private RedisTemplate<String, Object> jsonRedisTemplate;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void shouldBuildDraftWithMatchedTopicAndTrimmedNotes() {
        when(bookingTopicMapper.listActiveTopicNames())
                .thenReturn(List.of("Initial Consultation", "Career Planning"));

        AiBookingFormDraftVO result = bookingService.buildAiBookingDraft(
                8L,
                3L,
                15L,
                " career planning ",
                " Need guidance for internship applications. "
        );

        assertNotNull(result);
        assertEquals(8L, result.getCustomerId());
        assertEquals(3L, result.getSpecialistId());
        assertEquals(15L, result.getSlotId());
        assertEquals("Career Planning", result.getTopic());
        assertEquals("Need guidance for internship applications.", result.getCustomerNotes());
        assertEquals(2, result.getAvailableTopics().size());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void shouldFallbackTopicAndSanitizeInvalidInputs() {
        when(bookingTopicMapper.listActiveTopicNames())
                .thenReturn(List.of("Initial Consultation"));

        String overLimitNotes = "A".repeat(510) + "😀";
        AiBookingFormDraftVO result = bookingService.buildAiBookingDraft(
                9L,
                -2L,
                0L,
                "Unknown Topic",
                overLimitNotes
        );

        assertNotNull(result);
        assertEquals(9L, result.getCustomerId());
        assertNull(result.getSpecialistId());
        assertNull(result.getSlotId());
        assertEquals("Initial Consultation", result.getTopic());
        assertNotNull(result.getCustomerNotes());
        assertEquals(500, result.getCustomerNotes().length());
        assertFalse(result.getCustomerNotes().contains("😀"));
        assertTrue(result.getWarnings().contains("specialistId must be a positive number."));
        assertTrue(result.getWarnings().contains("slotId must be a positive number."));
        assertTrue(result.getWarnings().contains("Preferred topic is unavailable. Fallback topic is selected."));
        assertTrue(result.getWarnings().contains("Unsupported characters were removed from customer notes."));
        assertTrue(result.getWarnings().contains("customerNotes was truncated to 500 characters."));
    }
}

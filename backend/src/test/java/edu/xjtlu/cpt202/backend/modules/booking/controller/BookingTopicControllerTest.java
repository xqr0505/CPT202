package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.modules.booking.service.BookingTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingTopicControllerTest {

    private MockMvc mockMvc;
    private BookingTopicService bookingTopicService;

    @BeforeEach
    void setUp() {
        bookingTopicService = Mockito.mock(BookingTopicService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BookingTopicController(bookingTopicService))
                .build();
    }

    @Test
    void listBookingTopics_delegatesToServiceAndReturnsTopicNames() throws Exception {
        when(bookingTopicService.listActiveTopicNames()).thenReturn(List.of("Initial Consultation", "Follow Up"));

        mockMvc.perform(get("/api/v1/booking-topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("Initial Consultation"))
                .andExpect(jsonPath("$.data[1]").value("Follow Up"));

        verify(bookingTopicService).listActiveTopicNames();
    }
}

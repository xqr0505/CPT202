package edu.xjtlu.cpt202.backend.modules.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpecialistBookingControllerTest {

    private MockMvc mockMvc;

    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        bookingService = Mockito.mock(BookingService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SpecialistBookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        SecurityContextHolder.getContext().setAuthentication(specialistAuthentication());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPendingRequests_success() throws Exception {
        SpecialistPendingBookingVO item = new SpecialistPendingBookingVO();
        item.setId(1L);
        item.setCustomerName("Alice");
        item.setTopic("Career Planning");
        item.setRequestedStartTime(LocalDateTime.of(2026, 4, 20, 10, 0));
        item.setRequestedEndTime(LocalDateTime.of(2026, 4, 20, 11, 0));
        item.setSubmissionTime(LocalDateTime.of(2026, 4, 18, 9, 0));

        when(bookingService.listPendingRequestsForSpecialist(anyLong())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/specialist/booking-requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].customerName").value("Alice"));
    }

    @Test
    void getHandledRequests_success() throws Exception {
        SpecialistHandledBookingVO item = new SpecialistHandledBookingVO();
        item.setId(2L);
        item.setCustomerName("Bob");
        item.setStatus("REJECTED");
        item.setDecisionTime(LocalDateTime.of(2026, 4, 18, 12, 0));

        when(bookingService.listHandledRequestsForSpecialist(anyLong())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/specialist/booking-requests/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].status").value("REJECTED"));
    }

    @Test
    void getBookingRequestDetail_success() throws Exception {
        SpecialistBookingDetailVO detail = new SpecialistBookingDetailVO();
        detail.setId(3L);
        detail.setCustomerName("Carol");
        detail.setStatus("PENDING");

        when(bookingService.getBookingRequestDetailForSpecialist(anyLong(), anyLong())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/specialist/booking-requests/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName").value("Carol"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void approveBookingRequest_success() throws Exception {
        doNothing().when(bookingService).approveBookingRequest(anyLong(), anyLong());

        mockMvc.perform(post("/api/v1/specialist/booking-requests/3/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void rejectBookingRequest_success() throws Exception {
        doNothing().when(bookingService).rejectBookingRequest(anyLong(), anyLong(), any(SpecialistRejectBookingRequestDTO.class));

        mockMvc.perform(post("/api/v1/specialist/booking-requests/3/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "I am unavailable at that time."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void specialistForceCancelBooking_success() throws Exception {
        doNothing().when(bookingService).specialistForceCancelBooking(anyLong(), anyLong(), any(SpecialistForceCancelBookingRequestDTO.class));

        mockMvc.perform(post("/api/v1/specialist/booking-requests/3/force-cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancelReason": "Emergency leave",
                                  "releaseSlot": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Authentication specialistAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                2L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_SPECIALIST")
        );
    }
}

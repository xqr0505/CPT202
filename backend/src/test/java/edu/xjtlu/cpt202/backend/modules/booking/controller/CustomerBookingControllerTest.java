package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void getBookings_Success() throws Exception {
        BookingHistoryListVO item = BookingHistoryListVO.builder()
                .id(10L)
                .specialistName("Dr. Jane Smith")
                .status("PENDING")
                .amount(new BigDecimal("150.00"))
                .startTime(LocalDateTime.of(2026, 4, 5, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 5, 9, 45))
                .build();

        when(bookingService.listBookings(anyLong(), any()))
                .thenReturn(new PageResult<>(1, List.of(item)));

        mockMvc.perform(get("/api/v1/customer/bookings")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("timeScope", "UPCOMING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(10))
                .andExpect(jsonPath("$.data.list[0].specialistName").value("Dr. Jane Smith"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void createBooking_Success() throws Exception {
        when(bookingService.createBooking(anyLong(), any()))
                .thenReturn(new BookingCreateVO(101L, "PENDING"));

        mockMvc.perform(post("/api/v1/customer/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId": 1,
                                  "slotId": 12,
                                  "topic": "Career Planning",
                                  "customerNotes": "Need help with internship choices."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookingId").value(101))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void createBooking_ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/customer/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId": 1,
                                  "slotId": 12,
                                  "topic": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("topic is required"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void getBookingDetail_Success() throws Exception {
        BookingDetailVO detail = BookingDetailVO.builder()
                .id(10L)
                .specialistName("Dr. Jane Smith")
                .status("CONFIRMED")
                .amount(new BigDecimal("150.00"))
                .build();

        when(bookingService.getBookingDetail(10L, 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/customer/bookings/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.specialistName").value("Dr. Jane Smith"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}

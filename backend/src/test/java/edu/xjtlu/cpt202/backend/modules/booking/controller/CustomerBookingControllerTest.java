package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
    public void createBooking_Success() throws Exception {
        when(bookingService.createBooking(anyLong(), any()))
                .thenReturn(new BookingCreateVO(101L, "PENDING"));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        mockMvc.perform(post("/api/v1/customer/bookings")
                        .with(authentication(auth))
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
    public void createBooking_ValidationError() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        mockMvc.perform(post("/api/v1/customer/bookings")
                        .with(authentication(auth))
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
    public void customerCancellationQuote_Success() throws Exception {
        BookingCancelQuoteVO vo = BookingCancelQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .message("More than 24 hours to start, full refund")
                .bookingStartAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .orderAmount(new BigDecimal("100.00"))
                .refundAmount(new BigDecimal("100.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .build();
        when(bookingService.customerCancellationQuote(eq(55L), anyLong())).thenReturn(vo);

        mockMvc.perform(post("/api/v1/customer/bookings/55/cancel/quote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.policyType").value("FULL_REFUND"))
                .andExpect(jsonPath("$.data.refundAmount").value(100.0));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void customerRescheduleQuote_Success() throws Exception {
        BookingRescheduleQuoteVO vo = BookingRescheduleQuoteVO.builder()
                .allowed(true)
                .policyType("FULL_REFUND")
                .originalPrice(new BigDecimal("100.00"))
                .newPrice(new BigDecimal("120.00"))
                .priceDifference(new BigDecimal("20.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .refundAmount(new BigDecimal("0.00"))
                .payableAmount(new BigDecimal("20.00"))
                .build();
        when(bookingService.customerRescheduleQuote(eq(55L), eq(77L), anyLong())).thenReturn(vo);

        mockMvc.perform(post("/api/v1/customer/bookings/55/reschedule/quote")
                        .param("newSlotId", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.payableAmount").value(20.0))
                .andExpect(jsonPath("$.data.priceDifference").value(20.0));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void customerCancellationConfirm_Success() throws Exception {
        BookingCancelConfirmVO vo = BookingCancelConfirmVO.builder()
                .bookingId(55L)
                .bookingStatus("CANCELLED")
                .policyType("FULL_REFUND")
                .message("More than 24 hours to start, full refund")
                .refundAmount(new BigDecimal("100.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .build();
        when(bookingService.customerCancellationConfirm(eq(55L), anyLong())).thenReturn(vo);

        mockMvc.perform(post("/api/v1/customer/bookings/55/cancel/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookingId").value(55))
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(100.0));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void customerRescheduleConfirm_Success() throws Exception {
        BookingRescheduleConfirmVO vo = BookingRescheduleConfirmVO.builder()
                .bookingId(55L)
                .bookingStatus("PENDING")
                .policyType("FULL_REFUND")
                .message("More than 24 hours to start, no reschedule penalty")
                .priceDifference(new BigDecimal("20.00"))
                .penaltyAmount(new BigDecimal("0.00"))
                .refundAmount(new BigDecimal("0.00"))
                .payableAmount(new BigDecimal("20.00"))
                .build();
        when(bookingService.customerRescheduleConfirm(eq(55L), eq(77L), anyLong())).thenReturn(vo);

        mockMvc.perform(post("/api/v1/customer/bookings/55/reschedule/confirm")
                        .param("newSlotId", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookingId").value(55))
                .andExpect(jsonPath("$.data.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.payableAmount").value(20.0));
    }

    private Authentication customerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                1L,
                null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")
        );
    }

}

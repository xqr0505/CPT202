package edu.xjtlu.cpt202.backend.modules.booking.controller;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * @author QiranXiao
 * @date 2026/4/1
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    public void getUpcomingBookings_Success() throws Exception {
        // Arrange
        UpcomingBookingVO bookingVO = UpcomingBookingVO.builder()
                .id(1L)
                .specialistId(101L)
                .specialistName("Dr. John Doe")
                .specialistTitle("Senior Psychologist")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status("CONFIRMED")
                .build();

        List<UpcomingBookingVO> mockResponse = Collections.singletonList(bookingVO);

        when(bookingService.getUpcomingBookingsByCustomer(anyLong(), anyInt()))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].specialistName").value("Dr. John Doe"))
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getUpcomingBookings_ForbiddenForAdmin() throws Exception {
        // Roles should be CUSTOMER according to @PreAuthorize("hasRole('CUSTOMER')")
        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getUpcomingBookings_Unauthorized() throws Exception {
        // No user authenticated
        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}


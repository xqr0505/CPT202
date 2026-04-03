
// FIXME: 需要实现鉴权逻辑才能测试
//package edu.xjtlu.cpt202.backend.modules.booking.controller;
//
//import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
//import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
///**
// * @author QiranXiao
// * @date 2026/4/1
// *
// */
//@SpringBootTest
//@AutoConfigureMockMvc
//public class BookingControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private BookingService bookingService;
//
//    @Test
//    @WithMockUser(roles = "CUSTOMER")
//    public void getUpcomingBookings_Success() throws Exception {
//        // Arrange: mock service to return 3 bookings
//        List<UpcomingBookingVO> mockBookings = List.of(
//                UpcomingBookingVO.builder()
//                        .id(1L)
//                        .specialistName("Dr. John Doe")
//                        .serviceName("Mental Health Consultation")
//                        .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
//                        .today(false)
//                        .status("CONFIRMED")
//                        .build(),
//                UpcomingBookingVO.builder()
//                        .id(2L)
//                        .specialistName("Dr. Jane Smith")
//                        .serviceName("Career Counseling")
//                        .startTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0))
//                        .today(false)
//                        .status("CONFIRMED")
//                        .build(),
//                UpcomingBookingVO.builder()
//                        .id(3L)
//                        .specialistName("Dr. Alan Turing")
//                        .serviceName("AI Consultation")
//                        .startTime(LocalDateTime.now().plusDays(3).withHour(9).withMinute(30))
//                        .today(false)
//                        .status("CONFIRMED")
//                        .build()
//        );
//        when(bookingService.getUpcomingBookingsByCustomer(anyLong(), anyInt())).thenReturn(mockBookings);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data.length()").value(3))
//                .andExpect(jsonPath("$.data[0].specialistName").value("Dr. John Doe"))
//                .andExpect(jsonPath("$.data[1].specialistName").value("Dr. Jane Smith"))
//                .andExpect(jsonPath("$.data[2].specialistName").value("Dr. Alan Turing"))
//                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));
//
//        // Also test empty list
//        when(bookingService.getUpcomingBookingsByCustomer(anyLong(), anyInt())).thenReturn(Collections.emptyList());
//        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data.length()").value(0));
//    }
//
//    @Test
//    @WithMockUser(roles = "ADMIN")
//    public void getUpcomingBookings_ForbiddenForAdmin() throws Exception {
//        // Roles should be CUSTOMER according to @PreAuthorize("hasRole('CUSTOMER')")
//        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void getUpcomingBookings_Unauthorized() throws Exception {
//        // No user authenticated
//        mockMvc.perform(get("/api/v1/customer/dashboard/upcoming")
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isUnauthorized());
//    }
//}

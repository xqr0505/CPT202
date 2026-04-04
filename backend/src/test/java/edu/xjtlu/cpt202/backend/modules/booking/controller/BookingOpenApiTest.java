package edu.xjtlu.cpt202.backend.modules.booking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void apiDocs_ShouldContainCustomerBookingEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.paths['/api/v1/customer/bookings']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customer/bookings'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customer/bookings'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customer/bookings/{bookingId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customer/bookings/{bookingId}'].get").exists());
    }
}

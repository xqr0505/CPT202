package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleService scheduleService;

    @Test
    void createSlot_success() throws Exception {
        TimeSlotVO slot = buildTimeSlotVO();
        when(scheduleService.createSlot(any())).thenReturn(slot);

        mockMvc.perform(post("/api/specialist/schedule/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotDate": "2026-04-10",
                                  "startTime": "09:00:00",
                                  "endTime": "10:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value(TimeSlotStatusEnum.AVAILABLE.name()))
                .andExpect(jsonPath("$.data.statusDesc").value("Available"));
    }

    @Test
    void getWeeklySchedule_success() throws Exception {
        when(scheduleService.getWeeklySchedule(LocalDate.of(2026, 4, 6))).thenReturn(List.of(buildTimeSlotVO()));

        mockMvc.perform(get("/api/specialist/schedule/slots/weekly")
                        .param("weekStartDate", "2026-04-06")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].slotDate").value("2026-04-10"));
    }

    @Test
    void updateSlot_success() throws Exception {
        TimeSlotVO slot = buildTimeSlotVO();
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        slot.setStatusDesc("Available");

        when(scheduleService.updateSlot(eq(1L), any())).thenReturn(slot);

        mockMvc.perform(put("/api/specialist/schedule/slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "10:00:00",
                                  "endTime": "11:00:00",
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.statusDesc").value("Available"));
    }

    @Test
    void deleteSlot_success() throws Exception {
        doNothing().when(scheduleService).deleteSlot(1L);

        mockMvc.perform(delete("/api/specialist/schedule/slots/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getSlotById_success() throws Exception {
        when(scheduleService.getSlotById(1L)).thenReturn(buildTimeSlotVO());

        mockMvc.perform(get("/api/specialist/schedule/slots/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"));
    }

    private TimeSlotVO buildTimeSlotVO() {
        TimeSlotVO slot = new TimeSlotVO();
        slot.setId(1L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(LocalDate.of(2026, 4, 10));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        slot.setStatusDesc("Available");
        return slot;
    }
}

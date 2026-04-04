package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.RecurringRuleService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecurringRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecurringRuleService recurringRuleService;

    @Test
    void createRecurringRule_success() throws Exception {
        when(recurringRuleService.createRecurringRule(any())).thenReturn(buildRecurringRuleVO());

        mockMvc.perform(post("/api/specialist/schedule/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": 1,
                                  "startTime": "09:00:00",
                                  "endTime": "10:00:00",
                                  "effectiveEndDate": "2026-04-24"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.dayOfWeekDesc").value("Monday"));
    }

    @Test
    void getAllRecurringRules_success() throws Exception {
        when(recurringRuleService.getAllRecurringRules()).thenReturn(List.of(buildRecurringRuleVO()));

        mockMvc.perform(get("/api/specialist/schedule/rules")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].statusDesc").value("Active"));
    }

    @Test
    void getActiveRecurringRules_success() throws Exception {
        when(recurringRuleService.getActiveRecurringRules()).thenReturn(List.of(buildRecurringRuleVO()));

        mockMvc.perform(get("/api/specialist/schedule/rules/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value(1));
    }

    @Test
    void deleteRecurringRule_success() throws Exception {
        doNothing().when(recurringRuleService).deleteRecurringRule(1L);

        mockMvc.perform(delete("/api/specialist/schedule/rules/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private RecurringRuleVO buildRecurringRuleVO() {
        RecurringRuleVO rule = new RecurringRuleVO();
        rule.setId(1L);
        rule.setSpecialistId(1L);
        rule.setDayOfWeek(1);
        rule.setDayOfWeekDesc("Monday");
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(10, 0));
        rule.setEffectiveEndDate(LocalDate.of(2026, 4, 24));
        rule.setIsActive(1);
        rule.setStatusDesc("Active");
        return rule;
    }
}

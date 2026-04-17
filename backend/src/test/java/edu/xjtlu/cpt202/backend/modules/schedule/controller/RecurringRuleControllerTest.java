package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.RecurringRuleService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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

@ExtendWith(MockitoExtension.class)
class RecurringRuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecurringRuleService recurringRuleService;

    @InjectMocks
    private RecurringRuleController recurringRuleController;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json()
                        .modules(new JavaTimeModule())
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(recurringRuleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter)
                .build();
    }

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

    @Test
    void createRecurringRule_acceptsNullEndDate() throws Exception {
        when(recurringRuleService.createRecurringRule(any())).thenReturn(buildOpenEndedRecurringRuleVO());

        mockMvc.perform(post("/api/specialist/schedule/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": 1,
                                  "startTime": "09:00:00",
                                  "endTime": "10:00:00",
                                  "effectiveEndDate": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.effectiveEndDate").doesNotExist());
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

    private RecurringRuleVO buildOpenEndedRecurringRuleVO() {
        RecurringRuleVO rule = new RecurringRuleVO();
        rule.setId(2L);
        rule.setSpecialistId(1L);
        rule.setDayOfWeek(1);
        rule.setDayOfWeekDesc("Monday");
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(10, 0));
        rule.setEffectiveEndDate(null);
        rule.setIsActive(1);
        rule.setStatusDesc("Active");
        return rule;
    }
}

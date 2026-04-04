package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpecialistQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecialistQueryService specialistQueryService;

    @Test
    void listCategories_success() throws Exception {
        SpecialistCategoryVO category = new SpecialistCategoryVO();
        category.setId(1L);
        category.setName("Career");

        when(specialistQueryService.listCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Career"));
    }

    @Test
    void searchSpecialists_success() throws Exception {
        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(10L);
        specialist.setName("Lingxu");
        specialist.setConsultationFee(new BigDecimal("88.00"));

        when(specialistQueryService.searchSpecialists(any()))
                .thenReturn(new PageResult<>(1, List.of(specialist)));

        mockMvc.perform(get("/api/v1/specialists")
                        .param("pageNo", "1")
                        .param("pageSize", "12")
                        .param("sortBy", "recommended")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("Lingxu"));
    }

    @Test
    void searchSpecialists_serviceBusinessException() throws Exception {
        when(specialistQueryService.searchSpecialists(any()))
                .thenThrow(new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Unsupported sort option"));

        mockMvc.perform(get("/api/v1/specialists")
                        .param("sortBy", "random")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported sort option"));
    }

    @Test
    void getSpecialistDetail_success() throws Exception {
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(11L);
        detail.setName("Schedule Specialist");
        detail.setConsultationFee(new BigDecimal("120.00"));

        when(specialistQueryService.getSpecialistDetail(11L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/specialists/11")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.name").value("Schedule Specialist"));
    }

    @Test
    void listAvailability_success() throws Exception {
        SpecialistAvailabilityVO availability = new SpecialistAvailabilityVO();
        availability.setId(1L);
        availability.setSlotDate(LocalDate.of(2026, 4, 10));
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(10, 0));
        availability.setStatus("Open for booking");

        when(specialistQueryService.listAvailability(eq(11L), eq(LocalDate.of(2026, 4, 10))))
                .thenReturn(List.of(availability));

        mockMvc.perform(get("/api/v1/specialists/11/availability")
                        .param("date", "2026-04-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data[0].status").value("Open for booking"));
    }
}

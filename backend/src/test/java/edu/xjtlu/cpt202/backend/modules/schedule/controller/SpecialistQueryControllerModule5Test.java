package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SpecialistQueryControllerModule5Test {

    private MockMvc mockMvc;

    @Mock
    private SpecialistQueryService specialistQueryService;

    @InjectMocks
    private SpecialistQueryController specialistQueryController;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json()
                        .modules(new JavaTimeModule())
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build()
        );

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(specialistQueryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(jacksonConverter)
                .build();
    }

    @Test
    void listCategories_returnsModule5CategoryData() throws Exception {
        SpecialistCategoryVO category = new SpecialistCategoryVO();
        category.setId(1L);
        category.setName("Psychiatry");

        when(specialistQueryService.listCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Psychiatry"));
    }

    @Test
    void searchSpecialists_rejectsPageSizeAboveLimit() throws Exception {
        mockMvc.perform(get("/api/v1/specialists")
                        .param("sortBy", "recommended")
                        .param("pageSize", "25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("pageSize must not exceed 24"));

        verifyNoInteractions(specialistQueryService);
    }

    @Test
    void getSpecialistDetail_returnsSpecialistProfile() throws Exception {
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(9L);
        detail.setName("Dr. Helen");
        detail.setCategoryName("Psychiatry");
        detail.setConsultationFee(new BigDecimal("150.00"));

        when(specialistQueryService.getSpecialistDetail(9L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/specialists/9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.name").value("Dr. Helen"))
                .andExpect(jsonPath("$.data.categoryName").value("Psychiatry"));
    }

    @Test
    void listAvailability_returnsSlotsForSelectedDate() throws Exception {
        SpecialistAvailabilityVO availability = new SpecialistAvailabilityVO();
        availability.setId(3L);
        availability.setSlotDate(LocalDate.of(2026, 4, 20));
        availability.setStartTime(LocalTime.of(10, 0));
        availability.setEndTime(LocalTime.of(10, 30));
        availability.setStatus("Open for booking");

        when(specialistQueryService.listAvailability(eq(9L), eq(LocalDate.of(2026, 4, 20))))
                .thenReturn(List.of(availability));

        mockMvc.perform(get("/api/v1/specialists/9/availability")
                        .param("date", "2026-04-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data[0].status").value("Open for booking"));
    }

    @Test
    void searchSpecialists_returnsBusinessErrorForUnsupportedSort() throws Exception {
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
    void searchSpecialists_returnsPagedResults() throws Exception {
        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(5L);
        specialist.setName("Dr. Iris");
        specialist.setCategoryName("Psychiatry");
        specialist.setConsultationFee(new BigDecimal("120.00"));

        when(specialistQueryService.searchSpecialists(any()))
                .thenReturn(new PageResult<>(1, List.of(specialist)));

        mockMvc.perform(get("/api/v1/specialists")
                        .param("sortBy", "recommended")
                        .param("pageNo", "1")
                        .param("pageSize", "12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("Dr. Iris"))
                .andExpect(jsonPath("$.data.list[0].categoryName").value("Psychiatry"));
    }

    @Test
    void searchSpecialists_bindsCategoryKeywordAndDateFilters() throws Exception {
        when(specialistQueryService.searchSpecialists(any()))
                .thenReturn(new PageResult<>(0, List.of()));

        mockMvc.perform(get("/api/v1/specialists")
                        .param("categoryId", "3")
                        .param("keyword", "Helen")
                        .param("date", "2026-04-24")
                        .param("sortBy", "feeAsc")
                        .param("pageNo", "2")
                        .param("pageSize", "6")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));

        ArgumentCaptor<SpecialistSearchQueryDTO> queryCaptor =
                ArgumentCaptor.forClass(SpecialistSearchQueryDTO.class);
        verify(specialistQueryService).searchSpecialists(queryCaptor.capture());

        SpecialistSearchQueryDTO captured = queryCaptor.getValue();
        assertEquals(3L, captured.getCategoryId());
        assertEquals("Helen", captured.getKeyword());
        assertEquals(LocalDate.of(2026, 4, 24), captured.getDate());
        assertEquals("feeAsc", captured.getSortBy());
        assertEquals(2, captured.getPageNo());
        assertEquals(6, captured.getPageSize());
    }

    @Test
    void searchSpecialists_acceptsLevelSortForModule5Ranking() throws Exception {
        when(specialistQueryService.searchSpecialists(any()))
                .thenReturn(new PageResult<>(0, List.of()));

        mockMvc.perform(get("/api/v1/specialists")
                        .param("sortBy", "levelDesc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SpecialistSearchQueryDTO> queryCaptor =
                ArgumentCaptor.forClass(SpecialistSearchQueryDTO.class);
        verify(specialistQueryService).searchSpecialists(queryCaptor.capture());
        assertEquals("levelDesc", queryCaptor.getValue().getSortBy());
    }
}

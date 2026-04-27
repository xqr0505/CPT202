package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.exception.GlobalExceptionHandler;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistCreateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSpecialistControllerTest {

    private MockMvc mockMvc;
    private AdminSpecialistService adminSpecialistService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        adminSpecialistService = Mockito.mock(AdminSpecialistService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminSpecialistController(adminSpecialistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listSpecialists_delegatesQueryToService() throws Exception {
        AdminSpecialistListVO specialist = new AdminSpecialistListVO();
        specialist.setId(3L);
        specialist.setName("Dr Alice");
        when(adminSpecialistService.listSpecialists(any(AdminSpecialistListQueryDTO.class)))
                .thenReturn(new PageResult<>(1, List.of(specialist)));

        mockMvc.perform(get("/api/v1/admin/specialists")
                        .param("keyword", "Alice")
                        .param("status", "Active")
                        .param("pageNo", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(3));

        ArgumentCaptor<AdminSpecialistListQueryDTO> queryCaptor =
                ArgumentCaptor.forClass(AdminSpecialistListQueryDTO.class);
        verify(adminSpecialistService).listSpecialists(queryCaptor.capture());
        assertEquals("Alice", queryCaptor.getValue().getKeyword());
        assertEquals(2, queryCaptor.getValue().getPageNo());
    }

    @Test
    void createSpecialist_validatesAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dr Alice",
                                  "email": "alice@example.com",
                                  "categoryId": 1,
                                  "level": "Senior",
                                  "consultationFee": 100.00,
                                  "status": "Active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminSpecialistService).createSpecialist(any(AdminSpecialistCreateDTO.class));
    }

    @Test
    void createSpecialist_whenRequestInvalid_returnsValidationErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/v1/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "consultationFee": -1,
                                  "status": "Paused"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name cannot be empty")));

        verifyNoInteractions(adminSpecialistService);
    }

    @Test
    void getSpecialistDetail_returnsBusinessErrorWhenServiceThrows() throws Exception {
        when(adminSpecialistService.getSpecialistDetail(99L))
                .thenThrow(new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "Specialist not found"));

        mockMvc.perform(get("/api/v1/admin/specialists/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("Specialist not found"));

        verify(adminSpecialistService).getSpecialistDetail(99L);
    }

    @Test
    void updateSpecialistStatus_delegatesAndReturnsCancelledBookingCount() throws Exception {
        when(adminSpecialistService.updateSpecialistStatus(3L, "Inactive")).thenReturn(2);

        mockMvc.perform(patch("/api/v1/admin/specialists/3/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "Inactive"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));

        verify(adminSpecialistService).updateSpecialistStatus(3L, "Inactive");
    }

    @Test
    void getSpecialistDetail_delegatesToServiceAndReturnsDetail() throws Exception {
        AdminSpecialistDetailVO detail = new AdminSpecialistDetailVO();
        detail.setId(3L);
        detail.setName("Dr Alice");
        detail.setEmail("alice@example.com");
        detail.setConsultationFee(new BigDecimal("100.00"));
        when(adminSpecialistService.getSpecialistDetail(3L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/specialists/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));

        verify(adminSpecialistService).getSpecialistDetail(3L);
    }
}

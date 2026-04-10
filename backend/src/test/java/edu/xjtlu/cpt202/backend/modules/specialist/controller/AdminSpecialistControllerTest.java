package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSpecialistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminSpecialistService adminSpecialistService;

    @Test
    void listSpecialists_success() throws Exception {
        AdminSpecialistListVO vo = new AdminSpecialistListVO();
        vo.setId(10001L);
        vo.setName("Test Specialist");
        vo.setCategoryName("General Consultation");
        vo.setLevel("SENIOR");
        vo.setConsultationFee(new BigDecimal("120.00"));
        vo.setStatus("Active");

        PageResult<AdminSpecialistListVO> pageResult =
                new PageResult<>(1, Collections.singletonList(vo));

        when(adminSpecialistService.listSpecialists(any())).thenReturn(pageResult);

        mockMvc.perform(get("/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("Test Specialist"))
                .andExpect(jsonPath("$.data.list[0].status").value("Active"));
    }

    @Test
    void listSpecialists_success_withQueryParams() throws Exception {
        PageResult<AdminSpecialistListVO> pageResult =
                new PageResult<>(0, Collections.emptyList());

        when(adminSpecialistService.listSpecialists(any())).thenReturn(pageResult);

        mockMvc.perform(get("/admin/specialists")
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("status", "Active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void listSpecialists_badRequest_withInvalidStatus() throws Exception {
        mockMvc.perform(get("/admin/specialists")
                        .param("status", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be Active or Inactive"));
    }

    @Test
    void createSpecialist_success() throws Exception {
        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Test Specialist");
        request.setCategoryId(1L);
        request.setLevel("SENIOR");
        request.setConsultationFee(new BigDecimal("220.00"));
        request.setStatus("Active");

        doNothing().when(adminSpecialistService).createSpecialist(any(AdminSpecialistUpdateDTO.class));

        mockMvc.perform(post("/admin/specialists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

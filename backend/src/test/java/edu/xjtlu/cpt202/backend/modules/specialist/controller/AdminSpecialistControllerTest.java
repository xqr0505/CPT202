package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSpecialistControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}

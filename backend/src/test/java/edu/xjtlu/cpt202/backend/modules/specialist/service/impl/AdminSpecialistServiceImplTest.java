package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSpecialistServiceImplTest {

    @Mock
    private AdminSpecialistMapper adminSpecialistMapper;

    @InjectMocks
    private AdminSpecialistServiceImpl adminSpecialistService;

    @Test
    void listSpecialists_success_withDefaultPage() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();
        query.setPageNo(null);
        query.setPageSize(null);

        AdminSpecialistListVO vo = new AdminSpecialistListVO();
        vo.setId(10001L);
        vo.setName("Test Specialist");
        vo.setConsultationFee(new BigDecimal("120.00"));
        vo.setStatus("Active");

        IPage<AdminSpecialistListVO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(1);
        mockPage.setRecords(Collections.singletonList(vo));

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("Test Specialist", result.getList().get(0).getName());

        ArgumentCaptor<Page<AdminSpecialistListVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(adminSpecialistMapper).pageSpecialists(pageCaptor.capture(), eq(query));
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());
    }

    @Test
    void listSpecialists_success_withCustomPage() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();
        query.setPageNo(2);
        query.setPageSize(5);

        IPage<AdminSpecialistListVO> mockPage = new Page<>(2, 5);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());

        ArgumentCaptor<Page<AdminSpecialistListVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(adminSpecialistMapper).pageSpecialists(pageCaptor.capture(), eq(query));
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(5L, pageCaptor.getValue().getSize());
    }

    @Test
    void listSpecialists_emptyResult() {
        AdminSpecialistListQueryDTO query = new AdminSpecialistListQueryDTO();

        IPage<AdminSpecialistListVO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());

        when(adminSpecialistMapper.pageSpecialists(any(Page.class), eq(query))).thenReturn(mockPage);

        PageResult<AdminSpecialistListVO> result = adminSpecialistService.listSpecialists(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());
    }
}

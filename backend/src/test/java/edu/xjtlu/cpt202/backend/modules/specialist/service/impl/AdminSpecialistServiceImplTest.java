package edu.xjtlu.cpt202.backend.modules.specialist.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.specialist.mapper.AdminSpecialistMapper;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSpecialistServiceImplTest {

    @Mock
    private AdminSpecialistMapper adminSpecialistMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

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

    @Test
    void updateSpecialist_throwsBusinessException_whenFeeOutOfRange() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("300.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> adminSpecialistService.updateSpecialist(1L, request));

        assertEquals(400, exception.getCode());
        assertEquals("Consultation fee for CHIEF must be between 255.00 and 290.00", exception.getMessage());
        verify(adminSpecialistMapper, never()).updateSpecialistProfileById(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateSpecialist_success_whenFeeWithinRange() {
        AdminSpecialistDetailVO existing = new AdminSpecialistDetailVO();
        existing.setId(1L);

        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. Emily Chen");
        request.setCategoryId(1L);
        request.setLevel("CHIEF");
        request.setConsultationFee(new BigDecimal("260.00"));
        request.setStatus("Active");
        request.setAvatarUrl(null);

        when(adminSpecialistMapper.selectSpecialistDetailById(1L)).thenReturn(existing);
        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);
        when(adminSpecialistMapper.selectUserIdBySpecialistId(1L)).thenReturn(101L);
        when(adminSpecialistMapper.updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, "ACTIVE"
        )).thenReturn(1);

        adminSpecialistService.updateSpecialist(1L, request);

        verify(adminSpecialistMapper).updateSpecialistProfileById(
                1L, 1L, "CHIEF", new BigDecimal("260.00"), null, "ACTIVE"
        );
        verify(adminSpecialistMapper).updateUserFullNameById(101L, "Dr. Emily Chen");
    }

    @Test
    void createSpecialist_success() {
        AdminSpecialistUpdateDTO request = new AdminSpecialistUpdateDTO();
        request.setName("Dr. New Specialist");
        request.setCategoryId(1L);
        request.setLevel("SENIOR");
        request.setConsultationFee(new BigDecimal("220.00"));
        request.setStatus("Active");

        when(adminSpecialistMapper.selectCategoryCountById(1L)).thenReturn(1L);

        adminSpecialistService.createSpecialist(request);

        verify(userMapper).insert(any());
        verify(specialistProfileMapper).insert(any());
    }
}

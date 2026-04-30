package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.SpecialistQueryMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialistQueryServiceImplTest {

    @Mock
    private SpecialistQueryMapper specialistQueryMapper;

    @Mock
    private RecurringRuleServiceImpl recurringRuleServiceImpl;

    @InjectMocks
    private SpecialistQueryServiceImpl specialistQueryService;

    private static LocalDate futureDate(int days) {
        return LocalDate.now().plusDays(days);
    }

    @Test
    void searchSpecialists_success() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setPageNo(2);
        query.setPageSize(5);
        query.setSortBy("feeAsc");

        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(1001L);
        specialist.setName("Test Specialist");

        IPage<SpecialistSummaryVO> searchPage = new Page<>(2, 5);
        searchPage.setTotal(1);
        searchPage.setRecords(List.of(specialist));

        when(specialistQueryMapper.searchSpecialists(any(Page.class), eq(query), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(searchPage);

        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("Test Specialist", result.getList().get(0).getName());

        ArgumentCaptor<Page<SpecialistSummaryVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(specialistQueryMapper)
                .searchSpecialists(pageCaptor.capture(), eq(query), any(LocalDate.class), any(LocalTime.class));
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(5L, pageCaptor.getValue().getSize());
    }

    @Test
    void searchSpecialists_returnsEmptyListForPastDateFilter() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setDate(LocalDate.now().minusDays(1));
        query.setSortBy("recommended");

        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(query);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getList().size());
        verify(recurringRuleServiceImpl, never()).ensureSlotsGeneratedForDateRange(any(), any());
        verify(specialistQueryMapper, never())
                .searchSpecialists(any(Page.class), any(), any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void searchSpecialists_rejectsUnsupportedSort() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setSortBy("random");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> specialistQueryService.searchSpecialists(query));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Unsupported sort option", exception.getMessage());
    }

    @Test
    void getSpecialistDetail_success() {
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(88L);
        detail.setName("Lingxu");
        detail.setConsultationFee(new BigDecimal("100.00"));

        when(specialistQueryMapper.getSpecialistDetail(88L)).thenReturn(detail);

        SpecialistDetailVO result = specialistQueryService.getSpecialistDetail(88L);

        assertEquals(88L, result.getId());
        assertEquals("Lingxu", result.getName());
    }

    @Test
    void getSpecialistDetail_notFound() {
        when(specialistQueryMapper.getSpecialistDetail(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> specialistQueryService.getSpecialistDetail(99L));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void listAvailability_success() {
        LocalDate targetDate = futureDate(2);
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(66L);
        detail.setStatus("ACTIVE");

        SpecialistAvailabilityVO availability = new SpecialistAvailabilityVO();
        availability.setId(1L);
        availability.setSlotDate(targetDate);
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(10, 0));
        availability.setStatus("Open for booking");

        when(specialistQueryMapper.getSpecialistDetail(66L)).thenReturn(detail);
        when(specialistQueryMapper.listAvailabilityByDate(eq(66L), eq(targetDate), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(List.of(availability));

        List<SpecialistAvailabilityVO> result =
                specialistQueryService.listAvailability(66L, targetDate);

        assertEquals(1, result.size());
        assertEquals(LocalTime.of(9, 0), result.get(0).getStartTime());
    }
}

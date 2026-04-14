package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.SpecialistQueryMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialistQueryServiceImplModule5Test {

    private static ArgumentMatcher<Page<SpecialistSummaryVO>> matchesPage(long current, long size) {
        return page -> page.getCurrent() == current && page.getSize() == size;
    }

    @Mock
    private SpecialistQueryMapper specialistQueryMapper;

    @InjectMocks
    private SpecialistQueryServiceImpl specialistQueryService;

    @Test
    void listCategories_returnsMapperCategories() {
        SpecialistCategoryVO category = new SpecialistCategoryVO();
        category.setId(2L);
        category.setName("Cardiology");

        when(specialistQueryMapper.listCategories()).thenReturn(List.of(category));

        List<SpecialistCategoryVO> result = specialistQueryService.listCategories();

        assertEquals(1, result.size());
        assertEquals("Cardiology", result.get(0).getName());
    }

    @Test
    void searchSpecialists_usesDefaultPaginationForModule5Listing() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();

        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(11L);
        specialist.setName("Dr. Robin");
        specialist.setCategoryName("Psychiatry");
        specialist.setConsultationFee(new BigDecimal("180.00"));

        IPage<SpecialistSummaryVO> searchPage = new Page<>(1, 12);
        searchPage.setTotal(1);
        searchPage.setRecords(List.of(specialist));

        when(specialistQueryMapper.searchSpecialists(any(), eq(query)))
                .thenReturn(searchPage);

        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("Dr. Robin", result.getList().get(0).getName());

        verify(specialistQueryMapper).searchSpecialists(
                argThat(matchesPage(1L, 12L)),
                eq(query)
        );
    }

    @Test
    void searchSpecialists_acceptsCategoryKeywordDateAndFeeSortFilters() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setCategoryId(3L);
        query.setKeyword("Hel");
        query.setDate(LocalDate.of(2026, 4, 24));
        query.setSortBy("feeDesc");
        query.setPageNo(2);
        query.setPageSize(6);

        SpecialistSummaryVO specialist = new SpecialistSummaryVO();
        specialist.setId(15L);
        specialist.setName("Dr. Helen");
        specialist.setCategoryName("Psychiatry");
        specialist.setConsultationFee(new BigDecimal("220.00"));

        IPage<SpecialistSummaryVO> searchPage = new Page<>(2, 6);
        searchPage.setTotal(1);
        searchPage.setRecords(List.of(specialist));

        when(specialistQueryMapper.searchSpecialists(any(), eq(query)))
                .thenReturn(searchPage);

        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(query);

        assertEquals(1, result.getTotal());
        assertEquals("Dr. Helen", result.getList().get(0).getName());

        verify(specialistQueryMapper).searchSpecialists(
                argThat(matchesPage(2L, 6L)),
                eq(query)
        );
    }

    @Test
    void searchSpecialists_acceptsLevelDescendingSort() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setSortBy("levelDesc");

        IPage<SpecialistSummaryVO> searchPage = new Page<>(1, 12);
        searchPage.setTotal(0);
        searchPage.setRecords(List.of());

        when(specialistQueryMapper.searchSpecialists(any(), eq(query)))
                .thenReturn(searchPage);

        PageResult<SpecialistSummaryVO> result = specialistQueryService.searchSpecialists(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        verify(specialistQueryMapper).searchSpecialists(
                any(),
                eq(query)
        );
    }

    @Test
    void searchSpecialists_rejectsNullSortValue() {
        SpecialistSearchQueryDTO query = new SpecialistSearchQueryDTO();
        query.setSortBy(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> specialistQueryService.searchSpecialists(query));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("Unsupported sort option", exception.getMessage());
    }

    @Test
    void getSpecialistDetail_returnsNotFoundWhenMissing() {
        when(specialistQueryMapper.getSpecialistDetail(66L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> specialistQueryService.getSpecialistDetail(66L));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void listAvailability_returnsEmptyListWhenNoSlotsAvailable() {
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(18L);
        detail.setName("Dr. Empty");

        when(specialistQueryMapper.getSpecialistDetail(18L)).thenReturn(detail);
        when(specialistQueryMapper.listAvailabilityByDate(18L, LocalDate.of(2026, 4, 21)))
                .thenReturn(Collections.emptyList());

        List<SpecialistAvailabilityVO> result =
                specialistQueryService.listAvailability(18L, LocalDate.of(2026, 4, 21));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listAvailability_returnsMapperSlotsForChosenDate() {
        SpecialistDetailVO detail = new SpecialistDetailVO();
        detail.setId(22L);
        detail.setName("Dr. Ava");

        SpecialistAvailabilityVO availability = new SpecialistAvailabilityVO();
        availability.setId(7L);
        availability.setSlotDate(LocalDate.of(2026, 4, 22));
        availability.setStartTime(LocalTime.of(9, 30));
        availability.setEndTime(LocalTime.of(10, 0));
        availability.setStatus("Open for booking");

        when(specialistQueryMapper.getSpecialistDetail(22L)).thenReturn(detail);
        when(specialistQueryMapper.listAvailabilityByDate(22L, LocalDate.of(2026, 4, 22)))
                .thenReturn(List.of(availability));

        List<SpecialistAvailabilityVO> result =
                specialistQueryService.listAvailability(22L, LocalDate.of(2026, 4, 22));

        assertEquals(1, result.size());
        assertEquals(LocalTime.of(9, 30), result.get(0).getStartTime());
        assertEquals("Open for booking", result.get(0).getStatus());
    }
}

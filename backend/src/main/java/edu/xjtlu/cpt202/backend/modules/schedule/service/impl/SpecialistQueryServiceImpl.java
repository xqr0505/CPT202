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
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecialistQueryServiceImpl implements SpecialistQueryService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "recommended",
            "levelDesc",
            "feeAsc",
            "feeDesc"
    );

    private final SpecialistQueryMapper specialistQueryMapper;
    private final RecurringRuleServiceImpl recurringRuleServiceImpl;

    @Override
    public List<SpecialistCategoryVO> listCategories() {
        return specialistQueryMapper.listCategories();
    }

    @Override
    public PageResult<SpecialistSummaryVO> searchSpecialists(SpecialistSearchQueryDTO query) {
        validateSort(query.getSortBy());
        LocalDate today = LocalDate.now();
        if (query.getDate() != null) {
            if (query.getDate().isBefore(today)) {
                return new PageResult<>(0L, List.of());
            }
            recurringRuleServiceImpl.ensureSlotsGeneratedForDateRange(query.getDate(), query.getDate());
        }

        Page<SpecialistSummaryVO> page = new Page<>(query.getPageNo(), query.getPageSize());
        IPage<SpecialistSummaryVO> searchPage = specialistQueryMapper.searchSpecialists(
                page,
                query,
                today,
                LocalTime.now()
        );

        return new PageResult<>(searchPage.getTotal(), searchPage.getRecords());
    }

    @Override
    public SpecialistDetailVO getSpecialistDetail(Long specialistId) {
        SpecialistDetailVO detail = specialistQueryMapper.getSpecialistDetail(specialistId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
    }

    @Override
    public List<SpecialistAvailabilityVO> listAvailability(Long specialistId, LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return List.of();
        }
        SpecialistDetailVO detail = getSpecialistDetail(specialistId);
        if (!"ACTIVE".equals(detail.getStatus())) {
            return List.of();
        }
        recurringRuleServiceImpl.ensureSlotsGeneratedForSpecialist(specialistId, date, date);
        return specialistQueryMapper.listAvailabilityByDate(specialistId, date, today, LocalTime.now());
    }

    private void validateSort(String sortBy) {
        if (sortBy == null || !ALLOWED_SORTS.contains(sortBy)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Unsupported sort option");
        }
    }
}

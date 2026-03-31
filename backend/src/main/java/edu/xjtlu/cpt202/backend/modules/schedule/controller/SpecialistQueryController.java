package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.SpecialistSearchQueryDTO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistAvailabilityVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistCategoryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.SpecialistSummaryVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.SpecialistQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SpecialistQueryController {

    private final SpecialistQueryService specialistQueryService;

    @GetMapping("/categories")
    public Result<List<SpecialistCategoryVO>> listCategories() {
        return Result.success(specialistQueryService.listCategories());
    }

    @GetMapping("/specialists")
    public Result<PageResult<SpecialistSummaryVO>> searchSpecialists(@Valid SpecialistSearchQueryDTO query) {
        return Result.success(specialistQueryService.searchSpecialists(query));
    }

    @GetMapping("/specialists/{specialistId}")
    public Result<SpecialistDetailVO> getSpecialistDetail(@PathVariable Long specialistId) {
        return Result.success(specialistQueryService.getSpecialistDetail(specialistId));
    }

    @GetMapping("/specialists/{specialistId}/availability")
    public Result<List<SpecialistAvailabilityVO>> listAvailability(
            @PathVariable Long specialistId,
            @RequestParam
            @NotNull(message = "date is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return Result.success(specialistQueryService.listAvailability(specialistId, date));
    }
}

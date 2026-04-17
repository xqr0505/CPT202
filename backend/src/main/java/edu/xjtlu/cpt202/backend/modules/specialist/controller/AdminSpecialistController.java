package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import edu.xjtlu.cpt202.backend.common.enums.SpecialistLevelEnum;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistStatusUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistFeeChangeRecordVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.SpecialistLevelOptionVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/admin/specialists")
@RequiredArgsConstructor
public class AdminSpecialistController {

    private final AdminSpecialistService adminSpecialistService;

    @GetMapping
    public Result<PageResult<AdminSpecialistListVO>> listSpecialists(@Valid AdminSpecialistListQueryDTO query) {
        return Result.success(adminSpecialistService.listSpecialists(query));
    }

    @GetMapping("/levels")
    public Result<List<SpecialistLevelOptionVO>> listSpecialistLevels() {
        List<SpecialistLevelOptionVO> levels = Arrays.stream(SpecialistLevelEnum.values())
                .sorted(Comparator.comparing(SpecialistLevelEnum::getCode))
                .map(level -> new SpecialistLevelOptionVO(
                        level.name(),
                        level.name(),
                        level.getMinFee(),
                        level.getMaxFee()
                ))
                .toList();
        return Result.success(levels);
    }

    @PostMapping
    public Result<Void> createSpecialist(@Valid @RequestBody AdminSpecialistUpdateDTO request) {
        adminSpecialistService.createSpecialist(request);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AdminSpecialistDetailVO> getSpecialistDetail(@PathVariable Long id) {
        return Result.success(adminSpecialistService.getSpecialistDetail(id));
    }

    @GetMapping("/{id}/fee-change-records")
    public Result<List<SpecialistFeeChangeRecordVO>> listFeeChangeRecords(@PathVariable Long id) {
        return Result.success(adminSpecialistService.listFeeChangeRecords(id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateSpecialist(@PathVariable Long id, @Valid @RequestBody AdminSpecialistUpdateDTO request) {
        adminSpecialistService.updateSpecialist(id, request);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Integer> updateSpecialistStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminSpecialistStatusUpdateDTO request
    ) {
        return Result.success(adminSpecialistService.updateSpecialistStatus(id, request.getStatus()));
    }
}

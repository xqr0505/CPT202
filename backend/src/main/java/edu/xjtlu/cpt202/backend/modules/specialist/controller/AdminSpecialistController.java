package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistStatusUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistUpdateDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistDetailVO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/specialists")
@RequiredArgsConstructor
public class AdminSpecialistController {

    private final AdminSpecialistService adminSpecialistService;

    @GetMapping
    public Result<PageResult<AdminSpecialistListVO>> listSpecialists(@Valid AdminSpecialistListQueryDTO query) {
        return Result.success(adminSpecialistService.listSpecialists(query));
    }

    @GetMapping("/{id}")
    public Result<AdminSpecialistDetailVO> getSpecialistDetail(@PathVariable Long id) {
        return Result.success(adminSpecialistService.getSpecialistDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateSpecialist(@PathVariable Long id, @Valid @RequestBody AdminSpecialistUpdateDTO request) {
        adminSpecialistService.updateSpecialist(id, request);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateSpecialistStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminSpecialistStatusUpdateDTO request
    ) {
        adminSpecialistService.updateSpecialistStatus(id, request.getStatus());
        return Result.success();
    }
}

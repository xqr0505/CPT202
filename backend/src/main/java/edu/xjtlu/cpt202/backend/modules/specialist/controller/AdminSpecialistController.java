package edu.xjtlu.cpt202.backend.modules.specialist.controller;

import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.specialist.model.dto.AdminSpecialistListQueryDTO;
import edu.xjtlu.cpt202.backend.modules.specialist.model.vo.AdminSpecialistListVO;
import edu.xjtlu.cpt202.backend.modules.specialist.service.AdminSpecialistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}

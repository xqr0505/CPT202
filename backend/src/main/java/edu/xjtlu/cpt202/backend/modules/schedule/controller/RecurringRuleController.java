package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.common.annotation.Idempotent;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateRecurringRuleRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.RecurringRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for recurring availability rule management (Module 5).
 * @author Schedule Module Team
 */
@Tag(name = "Recurring Rules Management", description = "APIs for managing recurring availability rules")
@RestController
@RequestMapping("/api/specialist/schedule/rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;

    @Operation(summary = "Create a new recurring availability rule")
    @Idempotent
    @PreAuthorize("hasRole('SPECIALIST')")
    @PostMapping
    public Result<RecurringRuleVO> createRecurringRule(@Valid @RequestBody CreateRecurringRuleRequest request) {
        RecurringRuleVO rule = recurringRuleService.createRecurringRule(request);
        return Result.success(rule);
    }

    @Operation(summary = "Get all recurring rules for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    @GetMapping
    public Result<List<RecurringRuleVO>> getAllRecurringRules() {
        List<RecurringRuleVO> rules = recurringRuleService.getAllRecurringRules();
        return Result.success(rules);
    }

    @Operation(summary = "Get active recurring rules for current specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    @GetMapping("/active")
    public Result<List<RecurringRuleVO>> getActiveRecurringRules() {
        List<RecurringRuleVO> rules = recurringRuleService.getActiveRecurringRules();
        return Result.success(rules);
    }

    @Operation(summary = "Delete a recurring rule")
    @PreAuthorize("hasRole('SPECIALIST')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRecurringRule(@PathVariable Long id) {
        recurringRuleService.deleteRecurringRule(id);
        return Result.success();
    }
}

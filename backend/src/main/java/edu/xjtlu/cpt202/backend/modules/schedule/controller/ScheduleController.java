package edu.xjtlu.cpt202.backend.modules.schedule.controller;

import edu.xjtlu.cpt202.backend.common.annotation.Idempotent;
import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for time slot management (Module 4).
 * @author Schedule Module Team
 */
@Tag(name = "Schedule Management", description = "APIs for managing specialist time slots")
@RestController
@RequestMapping("/api/specialist/schedule/slots")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "Create a new time slot")
    @Idempotent
    @PreAuthorize("hasRole('SPECIALIST')")
    @PostMapping
    public Result<TimeSlotVO> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        TimeSlotVO slot = scheduleService.createSlot(request);
        return Result.success(slot);
    }

    @Operation(summary = "Get weekly schedule")
    @PreAuthorize("hasRole('SPECIALIST')")
    @GetMapping("/weekly")
    public Result<List<TimeSlotVO>> getWeeklySchedule(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        if (weekStartDate == null) {
            weekStartDate = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        }
        List<TimeSlotVO> schedule = scheduleService.getWeeklySchedule(weekStartDate);
        return Result.success(schedule);
    }

    @Operation(summary = "Update a time slot")
    @PreAuthorize("hasRole('SPECIALIST')")
    @PutMapping("/{id}")
    public Result<TimeSlotVO> updateSlot(@PathVariable Long id, @Valid @RequestBody UpdateSlotRequest request) {
        TimeSlotVO slot = scheduleService.updateSlot(id, request);
        return Result.success(slot);
    }

    @Operation(summary = "Delete a time slot")
    @PreAuthorize("hasRole('SPECIALIST')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSlot(@PathVariable Long id) {
        scheduleService.deleteSlot(id);
        return Result.success();
    }

    @Operation(summary = "Get a single time slot by ID")
    @PreAuthorize("hasRole('SPECIALIST')")
    @GetMapping("/{id}")
    public Result<TimeSlotVO> getSlotById(@PathVariable Long id) {
        TimeSlotVO slot = scheduleService.getSlotById(id);
        return Result.success(slot);
    }
}

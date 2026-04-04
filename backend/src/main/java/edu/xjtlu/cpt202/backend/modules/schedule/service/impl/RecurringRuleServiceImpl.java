package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.utils.BeanCopyUtils;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.AvailabilityRecurringRule;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.AvailabilityRecurringRuleMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateRecurringRuleRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.RecurringRuleService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum.*;

/**
 * Implementation of RecurringRuleService for recurring availability rule management.
 * @author Schedule Module Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringRuleServiceImpl implements RecurringRuleService {

    private final AvailabilityRecurringRuleMapper recurringRuleMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final SpecialistProfileMapper specialistProfileMapper;

    @Override
    @Transactional
    public RecurringRuleVO createRecurringRule(CreateRecurringRuleRequest request) {
        Long specialistId = getCurrentSpecialistId();

        validateTimeRange(request.getStartTime(), request.getEndTime());
        checkRuleConflict(specialistId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), null);

        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setSpecialistId(specialistId);
        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setEffectiveEndDate(request.getEffectiveEndDate());
        rule.setIsActive(1);

        recurringRuleMapper.insert(rule);
        log.info("Created recurring rule {} for specialist {}", rule.getId(), specialistId);

        generateTimeSlotsForRule(rule);

        return convertToVO(rule);
    }

    @Override
    public List<RecurringRuleVO> getAllRecurringRules() {
        Long specialistId = getCurrentSpecialistId();

        LambdaQueryWrapper<AvailabilityRecurringRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvailabilityRecurringRule::getSpecialistId, specialistId)
               .orderByDesc(AvailabilityRecurringRule::getCreatedAt);

        List<AvailabilityRecurringRule> rules = recurringRuleMapper.selectList(wrapper);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<RecurringRuleVO> getActiveRecurringRules() {
        Long specialistId = getCurrentSpecialistId();

        LambdaQueryWrapper<AvailabilityRecurringRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvailabilityRecurringRule::getSpecialistId, specialistId)
               .eq(AvailabilityRecurringRule::getIsActive, 1)
               .orderByAsc(AvailabilityRecurringRule::getDayOfWeek);

        List<AvailabilityRecurringRule> rules = recurringRuleMapper.selectList(wrapper);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRecurringRule(Long ruleId) {
        Long specialistId = getCurrentSpecialistId();

        AvailabilityRecurringRule existingRule = recurringRuleMapper.selectById(ruleId);
        if (existingRule == null) {
            throw new BusinessException(NOT_FOUND);
        }

        if (!existingRule.getSpecialistId().equals(specialistId)) {
            throw new BusinessException(FORBIDDEN);
        }

        deleteTimeSlotsByRuleId(ruleId);

        recurringRuleMapper.deleteById(ruleId);
        log.info("Deleted recurring rule {} and its generated time slots", ruleId);
    }

    private void generateTimeSlotsForRule(AvailabilityRecurringRule rule) {
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = rule.getEffectiveEndDate();

        if (currentDate.isAfter(endDate)) {
            return;
        }

        DayOfWeek targetDayOfWeek = DayOfWeek.of(rule.getDayOfWeek());
        LocalDate nextOccurrence = currentDate;

        while (nextOccurrence.getDayOfWeek() != targetDayOfWeek) {
            nextOccurrence = nextOccurrence.plusDays(1);
        }

        int slotsCreated = 0;
        while (!nextOccurrence.isAfter(endDate)) {
            if (hasTimeSlotConflict(rule.getSpecialistId(), nextOccurrence, rule.getStartTime(), rule.getEndTime())) {
                log.info("Skipped recurring slot on {} for rule {} due to conflict", nextOccurrence, rule.getId());
            } else {
                createTimeSlotFromRule(rule, nextOccurrence);
                slotsCreated++;
            }
            nextOccurrence = nextOccurrence.plusWeeks(1);
        }

        log.info("Generated {} time slots from recurring rule {}", slotsCreated, rule.getId());
    }

    private void createTimeSlotFromRule(AvailabilityRecurringRule rule, LocalDate date) {
        TimeSlot slot = new TimeSlot();
        slot.setSpecialistId(rule.getSpecialistId());
        slot.setRecurringRuleId(rule.getId());
        slot.setSlotDate(date);
        slot.setStartTime(rule.getStartTime());
        slot.setEndTime(rule.getEndTime());
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        timeSlotMapper.insert(slot);
    }

    private void deleteTimeSlotsByRuleId(Long ruleId) {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getRecurringRuleId, ruleId);
        timeSlotMapper.delete(wrapper);
        log.info("Deleted time slots generated by rule {}", ruleId);
    }

    private Long getCurrentSpecialistId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(UNAUTHORIZED);
        }
        Long specialistProfileId = specialistProfileMapper.selectIdByUserId(userId);
        if (specialistProfileId == null) {
            throw new BusinessException(NOT_FOUND.getCode(), "Specialist profile not found");
        }
        return specialistProfileId;
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start.isAfter(end) || start.equals(end)) {
            throw new BusinessException(PARAM_ERROR);
        }
    }

    private void checkRuleConflict(Long specialistId, Integer dayOfWeek,
                                    LocalTime startTime, LocalTime endTime, Long excludeRuleId) {
        LambdaQueryWrapper<AvailabilityRecurringRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvailabilityRecurringRule::getSpecialistId, specialistId)
               .eq(AvailabilityRecurringRule::getDayOfWeek, dayOfWeek)
               .eq(AvailabilityRecurringRule::getIsActive, 1)
               .and(excludeRuleId != null,
                       w -> w.ne(AvailabilityRecurringRule::getId, excludeRuleId))
               .apply("NOT (end_time <= {0} OR start_time >= {1})", startTime, endTime);

        if (recurringRuleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(PARAM_ERROR.getCode(), "Recurring rule overlaps with an existing rule");
        }
    }

    private boolean hasTimeSlotConflict(Long specialistId, LocalDate slotDate, LocalTime startTime, LocalTime endTime) {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getSpecialistId, specialistId)
               .eq(TimeSlot::getSlotDate, slotDate)
               .apply("NOT (end_time <= {0} OR start_time >= {1})", startTime, endTime);
        return timeSlotMapper.selectCount(wrapper) > 0;
    }

    private RecurringRuleVO convertToVO(AvailabilityRecurringRule rule) {
        RecurringRuleVO vo = BeanCopyUtils.copyBean(rule, RecurringRuleVO.class);
        vo.setDayOfWeekDesc(getDayOfWeekDesc(rule.getDayOfWeek()));
        vo.setStatusDesc(rule.getIsActive() == 1 ? "Active" : "Inactive");
        return vo;
    }

    private String getDayOfWeekDesc(Integer dayOfWeek) {
        if (dayOfWeek == null) {
            return "";
        }
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "";
        };
    }
}

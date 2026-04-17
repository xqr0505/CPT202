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

    private static final Long DEV_USER_ID = 1L;
    private static final int SLOT_DURATION_MINUTES = 30;
    private static final int OPEN_ENDED_GENERATION_WEEKS = 12;

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

        ensureSlotsGeneratedForRule(rule, LocalDate.now(), resolveGenerationEnd(rule, rule.getEffectiveEndDate()));

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

    public void ensureSlotsGeneratedForDateRange(LocalDate startDate, LocalDate endDate) {
        generateSlotsForMatchingRules(startDate, endDate, null);
    }

    public void ensureSlotsGeneratedForSpecialist(Long specialistId, LocalDate startDate, LocalDate endDate) {
        generateSlotsForMatchingRules(startDate, endDate, specialistId);
    }

    private void generateSlotsForMatchingRules(LocalDate startDate, LocalDate endDate, Long specialistId) {
        if (startDate == null || endDate == null) {
            return;
        }

        LocalDate generationStart = startDate.isBefore(LocalDate.now()) ? LocalDate.now() : startDate;
        if (generationStart.isAfter(endDate)) {
            return;
        }

        LambdaQueryWrapper<AvailabilityRecurringRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvailabilityRecurringRule::getIsActive, 1)
                .and(w -> w.isNull(AvailabilityRecurringRule::getEffectiveEndDate)
                        .or()
                        .ge(AvailabilityRecurringRule::getEffectiveEndDate, generationStart));
        if (specialistId != null) {
            wrapper.eq(AvailabilityRecurringRule::getSpecialistId, specialistId);
        }

        List<AvailabilityRecurringRule> rules = recurringRuleMapper.selectList(wrapper);
        for (AvailabilityRecurringRule rule : rules) {
            ensureSlotsGeneratedForRule(rule, generationStart, resolveGenerationEnd(rule, endDate));
        }
    }

    private void ensureSlotsGeneratedForRule(AvailabilityRecurringRule rule,
                                             LocalDate generationStart,
                                             LocalDate generationEnd) {
        if (generationStart == null || generationEnd == null || generationStart.isAfter(generationEnd)) {
            return;
        }

        DayOfWeek targetDayOfWeek = DayOfWeek.of(rule.getDayOfWeek());
        LocalDate nextOccurrence = generationStart;

        while (nextOccurrence.getDayOfWeek() != targetDayOfWeek) {
            nextOccurrence = nextOccurrence.plusDays(1);
        }

        int slotsCreated = 0;
        while (!nextOccurrence.isAfter(generationEnd)) {
            slotsCreated += createTimeSlotsForOccurrence(rule, nextOccurrence);
            nextOccurrence = nextOccurrence.plusWeeks(1);
        }

        if (slotsCreated > 0) {
            log.info("Generated {} time slots from recurring rule {}", slotsCreated, rule.getId());
        }
    }

    private int createTimeSlotsForOccurrence(AvailabilityRecurringRule rule, LocalDate date) {
        int createdSlots = 0;
        LocalTime slotStart = rule.getStartTime();

        while (slotStart.isBefore(rule.getEndTime())) {
            LocalTime slotEnd = slotStart.plusMinutes(SLOT_DURATION_MINUTES);
            if (slotEnd.isAfter(rule.getEndTime())) {
                break;
            }

            if (hasTimeSlotConflict(rule.getSpecialistId(), date, slotStart, slotEnd)) {
                log.info(
                        "Skipped recurring slot on {} from {} to {} for rule {} due to conflict",
                        date,
                        slotStart,
                        slotEnd,
                        rule.getId()
                );
            } else {
                createTimeSlotFromRule(rule, date, slotStart, slotEnd);
                createdSlots++;
            }

            slotStart = slotEnd;
        }

        return createdSlots;
    }

    private void createTimeSlotFromRule(AvailabilityRecurringRule rule,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime) {
        TimeSlot slot = new TimeSlot();
        slot.setSpecialistId(rule.getSpecialistId());
        slot.setRecurringRuleId(rule.getId());
        slot.setSlotDate(date);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
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
        Long userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (BusinessException ex) {
            if (!UNAUTHORIZED.getCode().equals(ex.getCode())) {
                throw ex;
            }
            userId = DEV_USER_ID;
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

    private LocalDate resolveGenerationEnd(AvailabilityRecurringRule rule, LocalDate requestedEndDate) {
        LocalDate boundedRequestedEnd = requestedEndDate != null
                ? requestedEndDate
                : LocalDate.now().plusWeeks(OPEN_ENDED_GENERATION_WEEKS);
        if (rule.getEffectiveEndDate() == null) {
            return boundedRequestedEnd;
        }
        return rule.getEffectiveEndDate().isBefore(boundedRequestedEnd)
                ? rule.getEffectiveEndDate()
                : boundedRequestedEnd;
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

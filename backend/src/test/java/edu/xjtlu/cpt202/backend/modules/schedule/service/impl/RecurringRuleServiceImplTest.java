package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.AvailabilityRecurringRule;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.AvailabilityRecurringRuleMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateRecurringRuleRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringRuleServiceImplTest {

    @Mock
    private AvailabilityRecurringRuleMapper recurringRuleMapper;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

    @InjectMocks
    private RecurringRuleServiceImpl recurringRuleService;

    @Test
    void createRecurringRule_success() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(LocalDate.now().plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(31L);
            return 1;
        });

        RecurringRuleVO result = recurringRuleService.createRecurringRule(request);

        assertNotNull(result);
        assertEquals(31L, result.getId());
        assertEquals(1L, result.getSpecialistId());
        assertEquals("Active", result.getStatusDesc());

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, times(4)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(LocalTime.of(9, 0), generatedSlots.get(0).getStartTime());
        assertEquals(LocalTime.of(9, 30), generatedSlots.get(0).getEndTime());
        assertEquals(LocalTime.of(9, 30), generatedSlots.get(1).getStartTime());
        assertEquals(LocalTime.of(10, 0), generatedSlots.get(1).getEndTime());
    }

    @Test
    void createRecurringRule_invalidTimeRange() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(1);
        request.setStartTime(LocalTime.of(11, 0));
        request.setEndTime(LocalTime.of(11, 0));
        request.setEffectiveEndDate(LocalDate.now().plusDays(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.createRecurringRule(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void createRecurringRule_conflict() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(1);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(LocalDate.now().plusDays(7));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.createRecurringRule(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getActiveRecurringRules_success() {
        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setId(41L);
        rule.setSpecialistId(1L);
        rule.setDayOfWeek(1);
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(10, 0));
        rule.setIsActive(1);

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of(rule));

        List<RecurringRuleVO> result = recurringRuleService.getActiveRecurringRules();

        assertEquals(1, result.size());
        assertEquals("Monday", result.get(0).getDayOfWeekDesc());
        assertEquals("Active", result.get(0).getStatusDesc());
    }

    @Test
    void deleteRecurringRule_notFound() {
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectById(44L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.deleteRecurringRule(44L));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void deleteRecurringRule_forbiddenWhenRuleBelongsToAnotherSpecialist() {
        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setId(45L);
        rule.setSpecialistId(2L);

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectById(45L)).thenReturn(rule);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.deleteRecurringRule(45L));

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void deleteRecurringRule_success() {
        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setId(46L);
        rule.setSpecialistId(1L);

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectById(46L)).thenReturn(rule);

        recurringRuleService.deleteRecurringRule(46L);

        verify(timeSlotMapper).delete(any());
        verify(recurringRuleMapper).deleteById(eq(46L));
    }

    @Test
    void createRecurringRule_skipsConflictingDatesAndCreatesRemainingSlots() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(LocalDate.now().plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(1L, 0L, 0L, 0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(52L);
            return 1;
        });

        recurringRuleService.createRecurringRule(request);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, times(3)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(TimeSlotStatusEnum.AVAILABLE.name(), generatedSlots.get(0).getStatus());
        assertEquals(LocalTime.of(9, 30), generatedSlots.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), generatedSlots.get(0).getEndTime());
    }

    @Test
    void recurringGeneratedSlots_areVisibleWhenViewingWeeklySchedule() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(LocalDate.now().plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L, 0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(61L);
            return 1;
        });

        recurringRuleService.createRecurringRule(request);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, times(4)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(4, generatedSlots.size());
        assertTrue(generatedSlots.stream().allMatch(slot -> TimeSlotStatusEnum.AVAILABLE.name().equals(slot.getStatus())));
        assertTrue(generatedSlots.stream().allMatch(slot -> Long.valueOf(61L).equals(slot.getRecurringRuleId())));
        assertTrue(generatedSlots.stream().allMatch(slot -> !slot.getSlotDate().isAfter(request.getEffectiveEndDate())));
    }

    @Test
    void createRecurringRule_withoutEndDate_generatesInitialRollingHorizon() {
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(71L);
            return 1;
        });

        RecurringRuleVO result = recurringRuleService.createRecurringRule(request);

        assertNotNull(result);
        assertEquals(71L, result.getId());
        assertEquals(null, result.getEffectiveEndDate());
        verify(timeSlotMapper, atLeastOnce()).insert(any(TimeSlot.class));
    }
}

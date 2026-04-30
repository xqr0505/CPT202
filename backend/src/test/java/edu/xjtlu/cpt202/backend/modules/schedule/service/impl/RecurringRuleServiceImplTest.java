package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.AvailabilityRecurringRuleException;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.AvailabilityRecurringRule;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.AvailabilityRecurringRuleExceptionMapper;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
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
    private AvailabilityRecurringRuleExceptionMapper recurringRuleExceptionMapper;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

    @InjectMocks
    private RecurringRuleServiceImpl recurringRuleService;

    private static LocalDate futureDate(int days) {
        return LocalDate.now().plusDays(days);
    }

    @Test
    void createRecurringRule_success() {
        LocalDate startDate = futureDate(1);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(startDate.getDayOfWeek().getValue());
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 0));
        request.setEffectiveEndDate(startDate.plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
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
        verify(timeSlotMapper, times(2)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(LocalTime.of(18, 0), generatedSlots.get(0).getStartTime());
        assertEquals(LocalTime.of(19, 0), generatedSlots.get(1).getEndTime());
        assertEquals(LocalTime.of(19, 0), generatedSlots.get(0).getEndTime());
    }

    @Test
    void createRecurringRule_invalidTimeRange() {
        LocalDate startDate = futureDate(1);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(1);
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(11, 0));
        request.setEndTime(LocalTime.of(11, 0));
        request.setEffectiveEndDate(startDate.plusDays(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.createRecurringRule(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void createRecurringRule_conflict() {
        LocalDate startDate = futureDate(14);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(1);
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(startDate.plusWeeks(4));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        AvailabilityRecurringRule existingRule = new AvailabilityRecurringRule();
        existingRule.setId(88L);
        existingRule.setSpecialistId(1L);
        existingRule.setDayOfWeek(1);
        existingRule.setEffectiveStartDate(startDate.plusDays(3));
        existingRule.setEffectiveEndDate(startDate.plusWeeks(5));
        existingRule.setStartTime(LocalTime.of(9, 30));
        existingRule.setEndTime(LocalTime.of(10, 30));
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of(existingRule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recurringRuleService.createRecurringRule(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getActiveRecurringRules_success() {
        LocalDate startDate = futureDate(1);
        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setId(41L);
        rule.setSpecialistId(1L);
        rule.setDayOfWeek(1);
        rule.setEffectiveStartDate(startDate);
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
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        recurringRuleService.deleteRecurringRule(46L);

        verify(timeSlotMapper).delete(any());
        verify(recurringRuleMapper).deleteById(eq(46L));
    }

    @Test
    void deleteRecurringRule_rejectsWhenGeneratedSlotsAreBookedOrLocked() {
        AvailabilityRecurringRule rule = new AvailabilityRecurringRule();
        rule.setId(47L);
        rule.setSpecialistId(1L);

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectById(47L)).thenReturn(rule);
        when(timeSlotMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> recurringRuleService.deleteRecurringRule(47L)
        );

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Recurring rule has booked or locked slots and cannot be deleted", exception.getMessage());
        verify(timeSlotMapper, never()).delete(any());
        verify(recurringRuleMapper, never()).deleteById(eq(47L));
    }

    @Test
    void createRecurringRule_skipsConflictingDatesAndCreatesRemainingSlots() {
        LocalDate startDate = futureDate(1);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(startDate.getDayOfWeek().getValue());
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(startDate.plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L, 0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(1L, 0L, 0L, 0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(52L);
            return 1;
        });

        recurringRuleService.createRecurringRule(request);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, times(1)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(TimeSlotStatusEnum.AVAILABLE.name(), generatedSlots.get(0).getStatus());
        assertEquals(LocalTime.of(9, 0), generatedSlots.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), generatedSlots.get(0).getEndTime());
    }

    @Test
    void recurringGeneratedSlots_areVisibleWhenViewingWeeklySchedule() {
        LocalDate startDate = futureDate(1);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(startDate.getDayOfWeek().getValue());
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(startDate.plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L, 0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(61L);
            return 1;
        });

        recurringRuleService.createRecurringRule(request);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, times(2)).insert(slotCaptor.capture());

        List<TimeSlot> generatedSlots = slotCaptor.getAllValues();
        assertEquals(2, generatedSlots.size());
        assertTrue(generatedSlots.stream().allMatch(slot -> TimeSlotStatusEnum.AVAILABLE.name().equals(slot.getStatus())));
        assertTrue(generatedSlots.stream().allMatch(slot -> Long.valueOf(61L).equals(slot.getRecurringRuleId())));
        assertTrue(generatedSlots.stream().allMatch(slot -> !slot.getSlotDate().isAfter(request.getEffectiveEndDate())));
    }

    @Test
    void createRecurringRule_withoutEndDate_generatesInitialRollingHorizon() {
        LocalDate startDate = futureDate(1);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(startDate.getDayOfWeek().getValue());
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
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

    @Test
    void recordRuleException_skipsDuplicateInsert() {
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(1L);

        recurringRuleService.recordRuleException(90L, LocalDate.of(2026, 4, 10));

        verify(recurringRuleExceptionMapper, times(0)).insert(any(AvailabilityRecurringRuleException.class));
    }

    @Test
    void createRecurringRule_futureStartDate_doesNotGenerateEarlierOccurrences() {
        LocalDate futureStart = LocalDate.now().plusWeeks(2);

        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(futureStart.getDayOfWeek().getValue());
        request.setEffectiveStartDate(futureStart);
        request.setStartTime(LocalTime.of(14, 0));
        request.setEndTime(LocalTime.of(15, 0));
        request.setEffectiveEndDate(futureStart.plusWeeks(1));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(72L);
            return 1;
        });

        recurringRuleService.createRecurringRule(request);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        verify(timeSlotMapper, atLeastOnce()).insert(slotCaptor.capture());
        assertTrue(slotCaptor.getAllValues().stream().allMatch(slot -> !slot.getSlotDate().isBefore(futureStart)));
    }

    @Test
    void createRecurringRule_allowsNonOverlappingDateRangesForSameWeeklyTime() {
        LocalDate requestStartDate = futureDate(60);
        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(1);
        request.setEffectiveStartDate(requestStartDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setEffectiveEndDate(requestStartDate.plusWeeks(4));

        AvailabilityRecurringRule existingRule = new AvailabilityRecurringRule();
        existingRule.setId(99L);
        existingRule.setSpecialistId(1L);
        existingRule.setDayOfWeek(1);
        existingRule.setEffectiveStartDate(requestStartDate.minusWeeks(8));
        existingRule.setEffectiveEndDate(requestStartDate.minusWeeks(4));
        existingRule.setStartTime(LocalTime.of(9, 0));
        existingRule.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of(existingRule));
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(100L);
            return 1;
        });

        RecurringRuleVO result = recurringRuleService.createRecurringRule(request);

        assertEquals(100L, result.getId());
        verify(recurringRuleMapper).insert(any(AvailabilityRecurringRule.class));
    }

    @Test
    void createRecurringRule_allowsArbitraryWeeklyTime() {
        LocalDate startDate = LocalDate.now()
                .plusDays(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));

        CreateRecurringRuleRequest request = new CreateRecurringRuleRequest();
        request.setDayOfWeek(DayOfWeek.TUESDAY.getValue());
        request.setEffectiveStartDate(startDate);
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 0));
        request.setEffectiveEndDate(startDate.plusWeeks(2));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(recurringRuleMapper.selectList(any())).thenReturn(List.of());
        when(recurringRuleExceptionMapper.selectCount(any())).thenReturn(0L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<AvailabilityRecurringRule> ruleCaptor = ArgumentCaptor.forClass(AvailabilityRecurringRule.class);
        when(recurringRuleMapper.insert(ruleCaptor.capture())).thenAnswer(invocation -> {
            AvailabilityRecurringRule rule = ruleCaptor.getValue();
            rule.setId(110L);
            return 1;
        });

        RecurringRuleVO result = recurringRuleService.createRecurringRule(request);

        assertEquals(110L, result.getId());
        verify(timeSlotMapper, atLeastOnce()).insert(any(TimeSlot.class));
    }
}

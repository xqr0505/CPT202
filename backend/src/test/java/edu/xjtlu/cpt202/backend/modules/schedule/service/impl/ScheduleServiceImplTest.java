package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @Mock
    private SpecialistProfileMapper specialistProfileMapper;

    @Mock
    private RecurringRuleServiceImpl recurringRuleServiceImpl;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private static LocalDate futureDate(int days) {
        return LocalDate.now().plusDays(days);
    }

    @Test
    void createSlot_success() {
        LocalDate slotDate = futureDate(2);
        CreateSlotRequest request = new CreateSlotRequest();
        request.setSlotDate(slotDate);
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        ArgumentCaptor<TimeSlot> slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
        when(timeSlotMapper.insert(slotCaptor.capture())).thenAnswer(invocation -> {
            TimeSlot slot = slotCaptor.getValue();
            slot.setId(101L);
            return 1;
        });

        TimeSlotVO result = scheduleService.createSlot(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(1L, result.getSpecialistId());
        assertEquals(slotDate, result.getSlotDate());
        assertEquals(LocalTime.of(18, 0), result.getStartTime());
        assertEquals(LocalTime.of(19, 0), result.getEndTime());
        assertEquals(TimeSlotStatusEnum.AVAILABLE.name(), result.getStatus());
        assertEquals("Available", result.getStatusDesc());
    }

    @Test
    void createSlot_invalidTimeRange() {
        LocalDate slotDate = futureDate(2);
        CreateSlotRequest request = new CreateSlotRequest();
        request.setSlotDate(slotDate);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        BusinessException exception = assertThrows(BusinessException.class, () -> scheduleService.createSlot(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void createSlot_conflict() {
        LocalDate slotDate = futureDate(2);
        CreateSlotRequest request = new CreateSlotRequest();
        request.setSlotDate(slotDate);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> scheduleService.createSlot(request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void createSlot_rejectsPastTimeSlot() {
        CreateSlotRequest request = new CreateSlotRequest();
        request.setSlotDate(LocalDate.now().minusDays(1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> scheduleService.createSlot(request));

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertEquals("Time slot must start in the future", exception.getMessage());
    }

    @Test
    void getWeeklySchedule_success() {
        TimeSlotVO slot = new TimeSlotVO();
        slot.setId(11L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(LocalDate.of(2026, 4, 6));
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(15, 0));
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setBookingStatus(BookingStatusEnum.PENDING.name());

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectWeeklyScheduleBySpecialistId(any(), any(), any())).thenReturn(List.of(slot));

        List<TimeSlotVO> result = scheduleService.getWeeklySchedule(LocalDate.of(2026, 4, 6));

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        assertEquals("Pending", result.get(0).getStatusDesc());
        assertEquals("Pending", result.get(0).getBookingStatusDesc());
    }

    @Test
    void getWeeklySchedule_includesCustomerInformationForBookedSlots() {
        TimeSlotVO slot = new TimeSlotVO();
        slot.setId(13L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(LocalDate.of(2026, 4, 7));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        slot.setBookingStatus(BookingStatusEnum.CONFIRMED.name());
        slot.setCustomerId(88L);
        slot.setCustomerName("Alice Zhang");
        slot.setCustomerEmail("alice@example.com");

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectWeeklyScheduleBySpecialistId(any(), any(), any())).thenReturn(List.of(slot));

        List<TimeSlotVO> result = scheduleService.getWeeklySchedule(LocalDate.of(2026, 4, 7));

        assertEquals(1, result.size());
        assertEquals("Confirmed", result.get(0).getStatusDesc());
        assertEquals("Confirmed", result.get(0).getBookingStatusDesc());
        assertEquals("Alice Zhang", result.get(0).getCustomerName());
        assertEquals("alice@example.com", result.get(0).getCustomerEmail());
    }

    @Test
    void updateSlot_successWithNewTimeAndStatus() {
        LocalDate slotDate = futureDate(3);
        TimeSlot slot = new TimeSlot();
        slot.setId(12L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(slotDate);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        UpdateSlotRequest request = new UpdateSlotRequest();
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 30));
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(12L)).thenReturn(slot);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        TimeSlotVO result = scheduleService.updateSlot(12L, request);

        assertEquals(LocalTime.of(18, 0), result.getStartTime());
        assertEquals(LocalTime.of(19, 30), result.getEndTime());
        assertEquals(TimeSlotStatusEnum.AVAILABLE.name(), result.getStatus());
        assertEquals("Available", result.getStatusDesc());
        verify(timeSlotMapper).updateById(slot);
    }

    @Test
    void updateSlot_rejectsConflictingTimeRangeAndPreservesOriginalSlot() {
        LocalDate slotDate = futureDate(3);
        TimeSlot slot = new TimeSlot();
        slot.setId(12L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(slotDate);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        UpdateSlotRequest request = new UpdateSlotRequest();
        request.setStartTime(LocalTime.of(9, 30));
        request.setEndTime(LocalTime.of(10, 30));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectById(12L)).thenReturn(slot);
        when(timeSlotMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.updateSlot(12L, request));

        assertEquals(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals(LocalTime.of(9, 0), slot.getStartTime());
        assertEquals(LocalTime.of(10, 0), slot.getEndTime());
    }

    @Test
    void updateSlot_notFound() {
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectById(12L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.updateSlot(12L, new UpdateSlotRequest()));

        assertEquals(ResultCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void updateSlot_forbiddenWhenSlotBelongsToAnotherSpecialist() {
        TimeSlot slot = new TimeSlot();
        slot.setId(12L);
        slot.setSpecialistId(2L);
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(12L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.updateSlot(12L, new UpdateSlotRequest()));

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void updateSlot_rejectsBookedSlot() {
        TimeSlot slot = new TimeSlot();
        slot.setId(12L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(12L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.updateSlot(12L, new UpdateSlotRequest()));

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    void deleteSlot_rejectsBookedSlot() {
        TimeSlot slot = new TimeSlot();
        slot.setId(15L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.BOOKED.name());
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(15L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.deleteSlot(15L));

        assertEquals(ResultCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    void deleteSlot_forbiddenWhenSlotBelongsToAnotherSpecialist() {
        TimeSlot slot = new TimeSlot();
        slot.setId(16L);
        slot.setSpecialistId(2L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(16L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.deleteSlot(16L));

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void getSlotById_forbiddenWhenSlotBelongsToAnotherSpecialist() {
        TimeSlot slot = new TimeSlot();
        slot.setId(20L);
        slot.setSpecialistId(99L);
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(20L)).thenReturn(slot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.getSlotById(20L));

        assertEquals(ResultCodeEnum.FORBIDDEN.getCode(), exception.getCode());
    }

    @Test
    void deleteSlot_success() {
        TimeSlot slot = new TimeSlot();
        slot.setId(21L);
        slot.setSpecialistId(1L);
        slot.setSlotDate(futureDate(2));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(21L)).thenReturn(slot);

        scheduleService.deleteSlot(21L);

        verify(timeSlotMapper).deleteById(eq(21L));
    }

    @Test
    void getSlotById_success() {
        TimeSlot slot = new TimeSlot();
        slot.setId(22L);
        slot.setSpecialistId(1L);
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());
        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);

        when(timeSlotMapper.selectById(22L)).thenReturn(slot);

        TimeSlotVO result = scheduleService.getSlotById(22L);

        assertNotNull(result);
        assertEquals(22L, result.getId());
        assertTrue(result.getStatusDesc().contains("Available"));
    }

    @Test
    void updateSlot_detachesRecurringOccurrenceWhenTimeChanges() {
        LocalDate slotDate = futureDate(4);
        TimeSlot slot = new TimeSlot();
        slot.setId(33L);
        slot.setSpecialistId(1L);
        slot.setRecurringRuleId(7L);
        slot.setSlotDate(slotDate);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        UpdateSlotRequest request = new UpdateSlotRequest();
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(11, 30));

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectById(33L)).thenReturn(slot);
        when(timeSlotMapper.selectCount(any())).thenReturn(0L);

        TimeSlotVO result = scheduleService.updateSlot(33L, request);

        assertEquals(LocalTime.of(10, 0), result.getStartTime());
        assertEquals(LocalTime.of(11, 30), result.getEndTime());
        assertEquals(null, slot.getRecurringRuleId());
        verify(recurringRuleServiceImpl).recordRuleException(7L, slotDate);
    }

    @Test
    void deleteSlot_recordsRecurringOccurrenceExceptionBeforeDelete() {
        LocalDate slotDate = futureDate(5);
        TimeSlot slot = new TimeSlot();
        slot.setId(34L);
        slot.setSpecialistId(1L);
        slot.setRecurringRuleId(8L);
        slot.setSlotDate(slotDate);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        when(specialistProfileMapper.selectIdByUserId(1L)).thenReturn(1L);
        when(timeSlotMapper.selectById(34L)).thenReturn(slot);

        scheduleService.deleteSlot(34L);

        verify(recurringRuleServiceImpl).recordRuleException(8L, slotDate);
        verify(timeSlotMapper).deleteById(34L);
    }
}

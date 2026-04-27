package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.utils.BeanCopyUtils;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.ScheduleService;
import edu.xjtlu.cpt202.backend.modules.user.mapper.SpecialistProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum.*;

/**
 * Implementation of ScheduleService for time slot management.
 * @author Schedule Module Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private static final Long DEV_USER_ID = 1L;

    private final TimeSlotMapper timeSlotMapper;
    private final SpecialistProfileMapper specialistProfileMapper;
    private final RecurringRuleServiceImpl recurringRuleServiceImpl;

    private static final Map<String, String> STATUS_DESC_MAP = Map.of(
        TimeSlotStatusEnum.AVAILABLE.name(), "Available",
        TimeSlotStatusEnum.BOOKED.name(), "Booked",
        TimeSlotStatusEnum.LOCKED.name(), "Locked"
    );

    private static final Map<String, String> BOOKING_STATUS_DESC_MAP = Map.of(
        BookingStatusEnum.PENDING.name(), "Pending",
        BookingStatusEnum.CONFIRMED.name(), "Confirmed",
        BookingStatusEnum.COMPLETED.name(), "Completed",
        BookingStatusEnum.CANCELLED.name(), "Cancelled"
    );

    @Override
    @Transactional
    public TimeSlotVO createSlot(CreateSlotRequest request) {
        Long specialistId = getCurrentSpecialistId();

        validateTimeRange(request.getStartTime(), request.getEndTime());
        checkTimeSlotConflict(specialistId, request.getSlotDate(), request.getStartTime(), request.getEndTime(), null);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setSpecialistId(specialistId);
        timeSlot.setSlotDate(request.getSlotDate());
        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        timeSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.name());

        timeSlotMapper.insert(timeSlot);
        log.info("Created time slot {} for specialist {}", timeSlot.getId(), specialistId);

        return convertToVO(timeSlot);
    }

    @Override
    public List<TimeSlotVO> getWeeklySchedule(LocalDate weekStartDate) {
        LocalDate weekEnd = weekStartDate.plusDays(6);
        Long specialistId = getCurrentSpecialistId();
        recurringRuleServiceImpl.ensureSlotsGeneratedForSpecialist(specialistId, weekStartDate, weekEnd);

        return timeSlotMapper.selectWeeklyScheduleBySpecialistId(specialistId, weekStartDate, weekEnd)
                .stream()
                .peek(this::enrichDisplayFields)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimeSlotVO updateSlot(Long slotId, UpdateSlotRequest request) {
        Long specialistId = getCurrentSpecialistId();

        TimeSlot existingSlot = timeSlotMapper.selectById(slotId);
        if (existingSlot == null) {
            throw new BusinessException(NOT_FOUND);
        }

        if (!existingSlot.getSpecialistId().equals(specialistId)) {
            throw new BusinessException(FORBIDDEN);
        }

        ensureSlotIsAvailable(existingSlot);

        LocalTime nextStartTime = request.getStartTime() != null ? request.getStartTime() : existingSlot.getStartTime();
        LocalTime nextEndTime = request.getEndTime() != null ? request.getEndTime() : existingSlot.getEndTime();
        boolean timeChanged = !nextStartTime.equals(existingSlot.getStartTime())
                || !nextEndTime.equals(existingSlot.getEndTime());

        validateTimeRange(nextStartTime, nextEndTime);
        checkTimeSlotConflict(specialistId, existingSlot.getSlotDate(), nextStartTime, nextEndTime, slotId);

        if (timeChanged && existingSlot.getRecurringRuleId() != null) {
            recurringRuleServiceImpl.recordRuleException(existingSlot.getRecurringRuleId(), existingSlot.getSlotDate());
            existingSlot.setRecurringRuleId(null);
        }

        if (request.getStartTime() != null) {
            existingSlot.setStartTime(nextStartTime);
        }
        if (request.getEndTime() != null) {
            existingSlot.setEndTime(nextEndTime);
        }

        if (request.getStatus() != null && !request.getStatus().equals(existingSlot.getStatus())) {
            throw new BusinessException(BAD_REQUEST.getCode(), "Changing slot status is not supported here");
        }

        timeSlotMapper.updateById(existingSlot);
        log.info("Updated time slot {}", slotId);

        return convertToVO(existingSlot);
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        Long specialistId = getCurrentSpecialistId();

        TimeSlot existingSlot = timeSlotMapper.selectById(slotId);
        if (existingSlot == null) {
            throw new BusinessException(NOT_FOUND);
        }

        if (!existingSlot.getSpecialistId().equals(specialistId)) {
            throw new BusinessException(FORBIDDEN);
        }

        ensureSlotIsAvailable(existingSlot);

        if (existingSlot.getRecurringRuleId() != null) {
            recurringRuleServiceImpl.recordRuleException(existingSlot.getRecurringRuleId(), existingSlot.getSlotDate());
        }

        timeSlotMapper.deleteById(slotId);
        log.info("Deleted time slot {}", slotId);
    }

    @Override
    public TimeSlotVO getSlotById(Long slotId) {
        Long specialistId = getCurrentSpecialistId();

        TimeSlot slot = timeSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException(NOT_FOUND);
        }

        if (!slot.getSpecialistId().equals(specialistId)) {
            throw new BusinessException(FORBIDDEN);
        }

        return convertToVO(slot);
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

    private void checkTimeSlotConflict(Long specialistId, LocalDate slotDate,
                                        LocalTime startTime, LocalTime endTime, Long excludeSlotId) {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getSpecialistId, specialistId)
               .eq(TimeSlot::getSlotDate, slotDate)
               .and(excludeSlotId != null,
                       w -> w.ne(TimeSlot::getId, excludeSlotId))
               .apply("NOT (end_time <= {0} OR start_time >= {1})", startTime, endTime);

        if (timeSlotMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(PARAM_ERROR.getCode(), "Time slot overlaps with an existing schedule entry");
        }
    }

    private TimeSlotVO convertToVO(TimeSlot slot) {
        TimeSlotVO vo = BeanCopyUtils.copyBean(slot, TimeSlotVO.class);
        enrichDisplayFields(vo);
        return vo;
    }

    private void enrichDisplayFields(TimeSlotVO vo) {
        vo.setStatusDesc(resolveStatusDesc(vo.getStatus(), vo.getBookingStatus()));
        if (vo.getBookingStatus() != null) {
            vo.setBookingStatusDesc(BOOKING_STATUS_DESC_MAP.getOrDefault(vo.getBookingStatus(), vo.getBookingStatus()));
        }
    }

    private String resolveStatusDesc(String slotStatus, String bookingStatus) {
        if (bookingStatus != null) {
            return BOOKING_STATUS_DESC_MAP.getOrDefault(bookingStatus, bookingStatus);
        }
        return STATUS_DESC_MAP.getOrDefault(slotStatus, slotStatus);
    }

    private void ensureSlotIsAvailable(TimeSlot existingSlot) {
        if (!TimeSlotStatusEnum.AVAILABLE.name().equals(existingSlot.getStatus())) {
            throw new BusinessException(BAD_REQUEST.getCode(), "Only available slots can be modified or deleted");
        }
    }
}

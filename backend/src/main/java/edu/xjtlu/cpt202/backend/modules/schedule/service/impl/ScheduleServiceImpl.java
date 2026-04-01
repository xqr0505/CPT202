package edu.xjtlu.cpt202.backend.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.utils.BeanCopyUtils;
import edu.xjtlu.cpt202.backend.common.utils.SecurityUtils;
import edu.xjtlu.cpt202.backend.modules.booking.enums.TimeSlotStatusEnum;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import edu.xjtlu.cpt202.backend.modules.schedule.service.ScheduleService;
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

    private final TimeSlotMapper timeSlotMapper;

    private static final Map<String, String> STATUS_DESC_MAP = Map.of(
        TimeSlotStatusEnum.AVAILABLE.getDesc(), "Available",
        TimeSlotStatusEnum.BOOKED.getDesc(), "Booked",
        TimeSlotStatusEnum.LOCKED.getDesc(), "Locked"
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
        timeSlot.setStatus(TimeSlotStatusEnum.AVAILABLE.getDesc());

        timeSlotMapper.insert(timeSlot);
        log.info("Created time slot {} for specialist {}", timeSlot.getId(), specialistId);

        return convertToVO(timeSlot);
    }

    @Override
    public List<TimeSlotVO> getWeeklySchedule(LocalDate weekStartDate) {
        LocalDate weekEnd = weekStartDate.plusDays(6);
        Long specialistId = getCurrentSpecialistId();

        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getSpecialistId, specialistId)
               .between(TimeSlot::getSlotDate, weekStartDate, weekEnd)
               .orderByAsc(TimeSlot::getSlotDate, TimeSlot::getStartTime);

        List<TimeSlot> slots = timeSlotMapper.selectList(wrapper);
        return slots.stream().map(this::convertToVO).collect(Collectors.toList());
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

        if (TimeSlotStatusEnum.BOOKED.getDesc().equals(existingSlot.getStatus())) {
            throw new BusinessException(BAD_REQUEST);
        }

        if (request.getStartTime() != null && request.getEndTime() != null) {
            validateTimeRange(request.getStartTime(), request.getEndTime());
            checkTimeSlotConflict(specialistId, existingSlot.getSlotDate(),
                    request.getStartTime(), request.getEndTime(), slotId);
            existingSlot.setStartTime(request.getStartTime());
            existingSlot.setEndTime(request.getEndTime());
        } else if (request.getStartTime() != null) {
            validateTimeRange(request.getStartTime(), existingSlot.getEndTime());
            checkTimeSlotConflict(specialistId, existingSlot.getSlotDate(),
                    request.getStartTime(), existingSlot.getEndTime(), slotId);
            existingSlot.setStartTime(request.getStartTime());
        } else if (request.getEndTime() != null) {
            validateTimeRange(existingSlot.getStartTime(), request.getEndTime());
            checkTimeSlotConflict(specialistId, existingSlot.getSlotDate(),
                    existingSlot.getStartTime(), request.getEndTime(), slotId);
            existingSlot.setEndTime(request.getEndTime());
        }

        if (request.getStatus() != null) {
            existingSlot.setStatus(request.getStatus());
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

        if (TimeSlotStatusEnum.BOOKED.getDesc().equals(existingSlot.getStatus())) {
            throw new BusinessException(BAD_REQUEST);
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
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(UNAUTHORIZED);
        }
        return userId;
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
            throw new BusinessException(PARAM_ERROR);
        }
    }

    private TimeSlotVO convertToVO(TimeSlot slot) {
        TimeSlotVO vo = BeanCopyUtils.copyBean(slot, TimeSlotVO.class);
        vo.setStatusDesc(STATUS_DESC_MAP.getOrDefault(slot.getStatus(), slot.getStatus()));
        return vo;
    }
}

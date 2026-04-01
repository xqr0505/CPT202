package edu.xjtlu.cpt202.backend.modules.schedule.service;

import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for time slot management.
 * @author Schedule Module Team
 */
public interface ScheduleService {

    /**
     * Create a new time slot for the current specialist.
     * @param request the create slot request
     * @return the created time slot VO
     */
    TimeSlotVO createSlot(CreateSlotRequest request);

    /**
     * Get weekly schedule for the current specialist.
     * @param weekStartDate the start date of the week (Monday)
     * @return list of time slots for the week
     */
    List<TimeSlotVO> getWeeklySchedule(LocalDate weekStartDate);

    /**
     * Update an existing time slot.
     * @param slotId the slot ID
     * @param request the update slot request (without ID)
     * @return the updated time slot VO
     */
    TimeSlotVO updateSlot(Long slotId, UpdateSlotRequest request);

    /**
     * Delete a time slot by ID.
     * @param slotId the slot ID
     */
    void deleteSlot(Long slotId);

    /**
     * Get a single time slot by ID.
     * @param slotId the slot ID
     * @return the time slot VO
     */
    TimeSlotVO getSlotById(Long slotId);
}

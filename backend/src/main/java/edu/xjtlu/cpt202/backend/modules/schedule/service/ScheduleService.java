package edu.xjtlu.cpt202.backend.modules.schedule.service;

import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.UpdateSlotRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    TimeSlotVO createSlot(CreateSlotRequest request);

    List<TimeSlotVO> getWeeklySchedule(LocalDate weekStartDate);

    TimeSlotVO updateSlot(Long slotId, UpdateSlotRequest request);

    void deleteSlot(Long slotId);

    TimeSlotVO getSlotById(Long slotId);
}

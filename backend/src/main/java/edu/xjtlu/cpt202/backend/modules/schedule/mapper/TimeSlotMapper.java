package edu.xjtlu.cpt202.backend.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for TimeSlot entity.
 * @author Schedule Module Team
 */
@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlot> {
}

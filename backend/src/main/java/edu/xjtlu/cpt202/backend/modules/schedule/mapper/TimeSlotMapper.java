package edu.xjtlu.cpt202.backend.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.TimeSlotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper interface for TimeSlot entity.
 * @author Schedule Module Team
 */
@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlot> {

    @Select("""
            SELECT
                ts.id AS id,
                ts.specialist_id AS specialistId,
                ts.recurring_rule_id AS recurringRuleId,
                ts.slot_date AS slotDate,
                ts.start_time AS startTime,
                ts.end_time AS endTime,
                ts.status AS status,
                ts.is_deleted AS isDeleted,
                b.id AS bookingId,
                b.status AS bookingStatus,
                b.customer_id AS customerId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS customerName,
                COALESCE(u.email, '') AS customerEmail,
                ts.created_at AS createdAt,
                ts.updated_at AS updatedAt
            FROM time_slots ts
            LEFT JOIN bookings b ON b.slot_id = ts.id AND b.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
            LEFT JOIN users u ON u.id = b.customer_id
            WHERE ts.specialist_id = #{specialistId}
              AND ts.slot_date BETWEEN #{weekStartDate} AND #{weekEndDate}
              AND ts.is_deleted = 0
            ORDER BY ts.slot_date ASC, ts.start_time ASC
            """)
    List<TimeSlotVO> selectWeeklyScheduleBySpecialistId(@Param("specialistId") Long specialistId,
                                                        @Param("weekStartDate") LocalDate weekStartDate,
                                                        @Param("weekEndDate") LocalDate weekEndDate);
}

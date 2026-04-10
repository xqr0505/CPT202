package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    List<UpcomingBookingVO> selectUpcomingBookings(@Param("customerId") Long customerId, @Param("status") String status, @Param("currentTime") LocalDateTime currentTime, @Param("limit") int limit);

    List<BookingItemVO> selectBookingList(@Param("customerId") Long customerId, @Param("tab") String tab, @Param("status") String status, @Param("currentTime") LocalDateTime currentTime, @Param("offset") long offset, @Param("pageSize") int pageSize);

    Long selectBookingListCount(@Param("customerId") Long customerId, @Param("tab") String tab, @Param("status") String status, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Select booking detail by booking ID.
     * @param bookingId the booking ID
     * @return Optional containing BookingDetailVO if found, empty otherwise
     */
    @Select("""
            SELECT
                b.id AS bookingId,
                b.status AS status,
                b.specialist_id AS specialistId,
                COALESCE(NULLIF(u.full_name, ''), u.email) AS specialistName,
                COALESCE(sp.avatar_url, '') AS specialistAvatar,
                DATE_FORMAT(ts.slot_date, '%Y-%m-%d') AS slotDate,
                DATE_FORMAT(ts.start_time, '%H:%i') AS startTime,
                DATE_FORMAT(ts.end_time, '%H:%i') AS endTime,
                b.price AS price,
                b.topic AS topic,
                b.customer_notes AS customerNotes
            FROM bookings b
            INNER JOIN time_slots ts ON b.slot_id = ts.id
            INNER JOIN specialist_profiles sp ON b.specialist_id = sp.id
            INNER JOIN users u ON sp.user_id = u.id
            WHERE b.id = #{bookingId}
            LIMIT 1
            """)
    Optional<BookingDetailVO> selectBookingDetailById(@Param("bookingId") Long bookingId);

}

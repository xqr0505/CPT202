package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    List<UpcomingBookingVO> selectUpcomingBookings(@Param("customerId") Long customerId, @Param("status") String status, @Param("currentTime") LocalDateTime currentTime, @Param("limit") int limit);

    List<BookingItemVO> selectBookingList(@Param("customerId") Long customerId, @Param("tab") String tab, @Param("currentTime") LocalDateTime currentTime, @Param("offset") long offset, @Param("pageSize") int pageSize);

    Long selectBookingListCount(@Param("customerId") Long customerId, @Param("tab") String tab, @Param("currentTime") LocalDateTime currentTime);

}

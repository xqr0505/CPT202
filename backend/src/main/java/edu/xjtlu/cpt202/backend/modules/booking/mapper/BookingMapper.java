package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
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

    List<UpcomingBookingVO> selectUpcomingBookings(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("limit") int limit
    );

    IPage<BookingHistoryListVO> listBookings(
            Page<BookingHistoryListVO> page,
            @Param("query") BookingHistoryQueryDTO query,
            @Param("customerId") Long customerId
    );

    BookingDetailVO getBookingDetail(
            @Param("bookingId") Long bookingId,
            @Param("customerId") Long customerId
    );

    List<SpecialistPendingBookingVO> selectPendingRequestsForSpecialist(
            @Param("currentUserId") Long currentUserId,
            @Param("status") String status
    );
}
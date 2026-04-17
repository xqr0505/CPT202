package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardHabitRawVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    List<BookingItemVO> selectBookingList(
            @Param("customerId") Long customerId,
            @Param("tab") String tab,
            @Param("status") String status,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    Long selectBookingListCount(
            @Param("customerId") Long customerId,
            @Param("tab") String tab,
            @Param("status") String status,
            @Param("currentTime") LocalDateTime currentTime
    );

    UsageSummaryVO selectUsageSummary(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<UsageSummaryVO.ConsultedExpertVO> selectConsultedExperts(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    DashboardStatisticsVO selectDashboardSummary(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<DashboardStatisticsVO.ConsultedExpertVO> selectDashboardConsultedExperts(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<DashboardStatisticsVO.TrendChartVO> selectDashboardTrendByDay(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<DashboardStatisticsVO.TrendChartVO> selectDashboardTrendByMonth(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<DashboardStatisticsVO.CategoryChartVO> selectDashboardCategoryData(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<DashboardHabitRawVO> selectDashboardHabitData(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<BookingDetailVO> selectBookingDetailById(@Param("bookingId") Long bookingId);

    List<SpecialistPendingBookingVO> selectPendingRequestsForSpecialist(
            @Param("currentUserId") Long currentUserId,
            @Param("status") String status
    );

    List<SpecialistHandledBookingVO> selectHandledRequestsForSpecialist(
            @Param("currentUserId") Long currentUserId
    );

    SpecialistBookingDetailVO selectBookingRequestDetailForSpecialist(
            @Param("bookingId") Long bookingId,
            @Param("currentUserId") Long currentUserId
    );

    Long countBookingOwnedBySpecialist(
            @Param("bookingId") Long bookingId,
            @Param("currentUserId") Long currentUserId
    );
}

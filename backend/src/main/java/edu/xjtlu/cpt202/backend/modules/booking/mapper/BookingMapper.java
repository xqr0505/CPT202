package edu.xjtlu.cpt202.backend.modules.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchItemVO;
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
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
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

    List<AiBookingSearchItemVO> selectAiBookingSearchList(@Param("customerId") Long customerId,
                                                          @Param("query") AiBookingSearchDTO query,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate,
                                                          @Param("currentTime") LocalDateTime currentTime,
                                                          @Param("useUpcomingTimeFilter") boolean useUpcomingTimeFilter,
                                                          @Param("useHistoryTimeFilter") boolean useHistoryTimeFilter,
                                                          @Param("sortAscending") boolean sortAscending,
                                                          @Param("limit") int limit);

    Long countAiBookingSearchList(@Param("customerId") Long customerId,
                                  @Param("query") AiBookingSearchDTO query,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("currentTime") LocalDateTime currentTime,
                                  @Param("useUpcomingTimeFilter") boolean useUpcomingTimeFilter,
                                  @Param("useHistoryTimeFilter") boolean useHistoryTimeFilter);

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
            @Param("status") String status,
            @Param("timeoutMinutes") long timeoutMinutes
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

    @Update("""
            UPDATE bookings
            SET status = #{status},
                cancelled_by = #{cancelledBy},
                cancel_reason = #{cancelReason},
                rejection_reason = #{rejectionReason},
                change_type = #{changeType},
                decision_time = #{decisionTime},
                updated_at = NOW()
            WHERE id = #{bookingId}
              AND specialist_id = #{specialistId}
              AND status IN ('PENDING', 'CONFIRMED')
            """)
    int updateSpecialistForceCancelIfCancellable(
            @Param("bookingId") Long bookingId,
            @Param("specialistId") Long specialistId,
            @Param("status") String status,
            @Param("cancelledBy") String cancelledBy,
            @Param("cancelReason") String cancelReason,
            @Param("rejectionReason") String rejectionReason,
            @Param("changeType") String changeType,
            @Param("decisionTime") LocalDateTime decisionTime
    );

    @Insert("""
            INSERT INTO refund_penalties (
                booking_id,
                refund_amount,
                penalty_amount,
                calculation_rule,
                status,
                processed_at
            ) VALUES (
                #{bookingId},
                #{refundAmount},
                #{penaltyAmount},
                #{calculationRule},
                #{status},
                NOW()
            )
            """)
    int insertRefundPenaltyRecord(
            @Param("bookingId") Long bookingId,
            @Param("refundAmount") BigDecimal refundAmount,
            @Param("penaltyAmount") BigDecimal penaltyAmount,
            @Param("calculationRule") String calculationRule,
            @Param("status") String status
    );

    @Update("""
            UPDATE bookings
            SET status = #{status},
                cancelled_by = #{cancelledBy},
                change_type = #{changeType},
                decision_time = #{decisionTime},
                updated_at = NOW()
            WHERE id = #{bookingId}
              AND customer_id = #{customerId}
              AND status IN ('PENDING', 'CONFIRMED')
            """)
    int updateCustomerCancelIfCancellable(
            @Param("bookingId") Long bookingId,
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("cancelledBy") String cancelledBy,
            @Param("changeType") String changeType,
            @Param("decisionTime") LocalDateTime decisionTime
    );

    @Update("""
            UPDATE bookings
            SET slot_id = #{newSlotId},
                status = #{status},
                change_type = #{changeType},
                decision_time = #{decisionTime},
                updated_at = NOW()
            WHERE id = #{bookingId}
              AND customer_id = #{customerId}
              AND status IN ('PENDING', 'CONFIRMED')
            """)
    int updateCustomerRescheduleIfCancellable(
            @Param("bookingId") Long bookingId,
            @Param("customerId") Long customerId,
            @Param("newSlotId") Long newSlotId,
            @Param("status") String status,
            @Param("changeType") String changeType,
            @Param("decisionTime") LocalDateTime decisionTime
    );
}

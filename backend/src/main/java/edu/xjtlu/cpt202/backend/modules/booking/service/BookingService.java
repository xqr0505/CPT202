package edu.xjtlu.cpt202.backend.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.DashboardQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistForceCancelBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.UsageSummaryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleConfirmVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingRescheduleQuoteVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.DashboardStatisticsVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingFormDraftVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UsageSummaryVO;

import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
public interface BookingService extends IService<Booking> {

    List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit);

    BookingCreateVO createBooking(Long customerId, BookingCreateDTO createDTO);

    AiBookingFormDraftVO buildAiBookingDraft(
            Long customerId,
            Long specialistId,
            Long slotId,
            String preferredTopic,
            String customerNotes
    );

    PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto);

    UsageSummaryVO getUsageSummary(UsageSummaryQueryDTO queryDTO);

    DashboardStatisticsVO getDashboardStatistics(DashboardQueryDTO queryDTO);

    BookingDetailVO getBookingDetailById(Long bookingId, Long currentCustomerId);

    BookingCancelQuoteVO customerCancellationQuote(Long bookingId, Long currentCustomerId);

    BookingCancelConfirmVO customerCancellationConfirm(Long bookingId, Long currentCustomerId);

    BookingRescheduleQuoteVO customerRescheduleQuote(Long bookingId, Long newSlotId, Long currentCustomerId);

    BookingRescheduleConfirmVO customerRescheduleConfirm(Long bookingId, Long newSlotId, Long currentCustomerId);

    List<SpecialistPendingBookingVO> listPendingRequestsForSpecialist(Long currentUserId);

    List<SpecialistHandledBookingVO> listHandledRequestsForSpecialist(Long currentUserId);

    SpecialistBookingDetailVO getBookingRequestDetailForSpecialist(Long bookingId, Long currentUserId);

    void approveBookingRequest(Long bookingId, Long currentUserId);

    void rejectBookingRequest(Long bookingId, Long currentUserId, SpecialistRejectBookingRequestDTO requestDTO);

    void specialistForceCancelBooking(
            Long bookingId,
            Long currentUserId,
            SpecialistForceCancelBookingRequestDTO requestDTO
    );
}

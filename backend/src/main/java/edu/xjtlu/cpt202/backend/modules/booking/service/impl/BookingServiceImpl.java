package edu.xjtlu.cpt202.backend.modules.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.enums.BookingStatusEnum;
import edu.xjtlu.cpt202.backend.modules.booking.mapper.BookingMapper;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingHistoryQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.SpecialistRejectBookingRequestDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistBookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistHandledBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.SpecialistPendingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import edu.xjtlu.cpt202.backend.modules.schedule.entity.TimeSlot;
import edu.xjtlu.cpt202.backend.modules.schedule.mapper.TimeSlotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final BookingMapper bookingMapper;
    private final TimeSlotMapper timeSlotMapper;

    public BookingServiceImpl(BookingMapper bookingMapper, TimeSlotMapper timeSlotMapper) {
        this.bookingMapper = bookingMapper;
        this.timeSlotMapper = timeSlotMapper;
    }

    @Override
    public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
        return bookingMapper.selectUpcomingBookings(
                customerId,
                BookingStatusEnum.CONFIRMED.name(),
                LocalDateTime.now(),
                limit
        );
    }

    @Override
    public PageResult<BookingHistoryListVO> listBookings(Long customerId, BookingHistoryQueryDTO queryDTO) {
        Page<BookingHistoryListVO> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        IPage<BookingHistoryListVO> resultPage = bookingMapper.listBookings(page, queryDTO, customerId);
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public BookingDetailVO getBookingDetail(Long bookingId, Long customerId) {
        BookingDetailVO detail = bookingMapper.getBookingDetail(bookingId, customerId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
    }

    @Override
    public List<SpecialistPendingBookingVO> listPendingRequestsForSpecialist(Long currentUserId) {
        return bookingMapper.selectPendingRequestsForSpecialist(
                currentUserId,
                BookingStatusEnum.PENDING.name()
        );
    }

    @Override
    public List<SpecialistHandledBookingVO> listHandledRequestsForSpecialist(Long currentUserId) {
        return bookingMapper.selectHandledRequestsForSpecialist(currentUserId);
    }

    @Override
    public SpecialistBookingDetailVO getBookingRequestDetailForSpecialist(Long bookingId, Long currentUserId) {
        validateBookingOwnershipForSpecialist(bookingId, currentUserId);
        SpecialistBookingDetailVO detail = bookingMapper.selectBookingRequestDetailForSpecialist(bookingId, currentUserId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        return detail;
    }

    @Override
    @Transactional
    public void approveBookingRequest(Long bookingId, Long currentUserId) {
        Booking booking = loadPendingBookingForSpecialist(bookingId, currentUserId);
        booking.setStatus(BookingStatusEnum.CONFIRMED.name());
        booking.setDecisionTime(LocalDateTime.now());
        booking.setRejectionReason(null);
        bookingMapper.updateById(booking);

        TimeSlot timeSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (timeSlot != null) {
            timeSlot.setStatus("BOOKED");
            timeSlotMapper.updateById(timeSlot);
        }
    }

    @Override
    @Transactional
    public void rejectBookingRequest(Long bookingId, Long currentUserId, SpecialistRejectBookingRequestDTO requestDTO) {
        Booking booking = loadPendingBookingForSpecialist(bookingId, currentUserId);
        booking.setStatus(BookingStatusEnum.CANCELLED.name());
        booking.setDecisionTime(LocalDateTime.now());
        booking.setRejectionReason(requestDTO.getRejectionReason().trim());
        booking.setCancelledBy("SPECIALIST");
        booking.setCancelReason(requestDTO.getRejectionReason().trim());
        booking.setChangeType("REJECT");
        bookingMapper.updateById(booking);

        TimeSlot timeSlot = timeSlotMapper.selectById(booking.getSlotId());
        if (timeSlot != null) {
            timeSlot.setStatus("AVAILABLE");
            timeSlotMapper.updateById(timeSlot);
        }
    }

    private Booking loadPendingBookingForSpecialist(Long bookingId, Long currentUserId) {
        validateBookingOwnershipForSpecialist(bookingId, currentUserId);
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (!BookingStatusEnum.PENDING.name().equals(booking.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "Only pending booking requests can be handled");
        }
        return booking;
    }

    private void validateBookingOwnershipForSpecialist(Long bookingId, Long currentUserId) {
        Long count = bookingMapper.countBookingOwnedBySpecialist(bookingId, currentUserId);
        if (count == null || count == 0) {
            Booking booking = bookingMapper.selectById(bookingId);
            if (booking == null) {
                throw new BusinessException(ResultCodeEnum.NOT_FOUND);
            }
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
    }
}
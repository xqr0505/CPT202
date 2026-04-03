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
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingDetailVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingHistoryListVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;
import edu.xjtlu.cpt202.backend.modules.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author QiranXiao
 * @since 2026/4/1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final BookingMapper bookingMapper;

    @Override
    public List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<UpcomingBookingVO> result = bookingMapper.selectUpcomingBookings(customerId, BookingStatusEnum.CONFIRMED.name(), now, limit);

        if (result != null) {
            result.forEach(booking -> {
                booking.setToday(booking.getStartTime().toLocalDate().isEqual(now.toLocalDate()));
            });
        }

        return result != null ? result : List.of();
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
}

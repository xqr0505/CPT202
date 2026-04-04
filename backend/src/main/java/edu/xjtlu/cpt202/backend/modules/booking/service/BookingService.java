package edu.xjtlu.cpt202.backend.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.xjtlu.cpt202.backend.common.result.PageResult;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingCreateDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.dto.BookingPageQueryDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCreateVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingItemVO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;

import java.util.List;

/**
 * @author QiranXiao
 * @date 2026/4/1
 */
public interface BookingService extends IService<Booking> {
    List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit);

    BookingCreateVO createBooking(Long customerId, BookingCreateDTO createDTO);

    PageResult<BookingItemVO> getBookingList(Long customerId, BookingPageQueryDTO dto);
}

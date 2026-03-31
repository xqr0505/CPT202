package edu.xjtlu.cpt202.backend.modules.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.xjtlu.cpt202.backend.modules.booking.model.entity.Booking;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.UpcomingBookingVO;

import java.util.List;

public interface BookingService extends IService<Booking> {
    List<UpcomingBookingVO> getUpcomingBookingsByCustomer(Long customerId, int limit);
}


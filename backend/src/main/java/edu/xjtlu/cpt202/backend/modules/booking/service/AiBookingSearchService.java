package edu.xjtlu.cpt202.backend.modules.booking.service;

import edu.xjtlu.cpt202.backend.modules.booking.model.dto.AiBookingSearchDTO;
import edu.xjtlu.cpt202.backend.modules.booking.model.vo.AiBookingSearchResultVO;


/**
 *
 * @author QiranXiao
 * @since 2026/4/17
 */
public interface AiBookingSearchService {

    AiBookingSearchResultVO searchCustomerBookings(Long customerId, AiBookingSearchDTO queryDTO);
}

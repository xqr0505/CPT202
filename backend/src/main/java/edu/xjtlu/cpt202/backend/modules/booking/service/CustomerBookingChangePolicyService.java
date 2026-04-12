package edu.xjtlu.cpt202.backend.modules.booking.service;

import edu.xjtlu.cpt202.backend.modules.booking.model.vo.BookingCancelQuoteVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户侧取消 / 改期相关的纯规则试算（不落库）。
 */
public interface CustomerBookingChangePolicyService {

    /**
     * 根据当前预约状态、时段开始时间、订单金额，试算客户取消时的退款与违约金。
     *
     * @param bookingStatus 预约状态名，如 PENDING / CONFIRMED
     * @param slotStartAt   时段开始时间
     * @param now           当前时间（可注入便于测试）
     * @param orderAmount   订单上的咨询费金额
     */
    BookingCancelQuoteVO customerCancellationQuote(
            String bookingStatus,
            LocalDateTime slotStartAt,
            LocalDateTime now,
            BigDecimal orderAmount
    );
}

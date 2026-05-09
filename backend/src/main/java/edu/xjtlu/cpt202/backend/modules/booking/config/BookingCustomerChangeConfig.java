package edu.xjtlu.cpt202.backend.modules.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 客户取消 / 改期：时间窗与违约金比例。
 * 与 application.yml 中 {@code booking.customer-change} 对齐。
 */
@Data
@Component
@ConfigurationProperties(prefix = "booking.customer-change")
public class BookingCustomerChangeConfig {

    /** 距开始大于该小时数：全额退款 */
    private int fullRefundLeadHours = 24;

    /** 距开始小于等于该小时数：不可取消 / 改期 */
    private int minLeadHours = 2;

    /**
     * (minLead, fullRefundLead] 区间内：违约金 = 订单金额 × 比例；退款 = 订单金额 − 违约金。
     * YAML 键名 {@code late-window-penalty-ratio} 绑定到本字段。
     */
    private BigDecimal lateWindowPenaltyRatio = new BigDecimal("0.30");

    public BigDecimal getPenaltyRatio() {
        return lateWindowPenaltyRatio;
    }

    public void setPenaltyRatio(BigDecimal ratio) {
        this.lateWindowPenaltyRatio = ratio;
    }
}

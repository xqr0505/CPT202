package edu.xjtlu.cpt202.backend.modules.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "booking.customer-change")
public class BookingCustomerChangeConfig {

    private int fullRefundLeadHours = 24;

    private int minLeadHours = 2;

    private BigDecimal lateWindowPenaltyRatio = new BigDecimal("0.30");

    public BigDecimal getPenaltyRatio() {
        return lateWindowPenaltyRatio;
    }

    public void setPenaltyRatio(BigDecimal ratio) {
        this.lateWindowPenaltyRatio = ratio;
    }
}

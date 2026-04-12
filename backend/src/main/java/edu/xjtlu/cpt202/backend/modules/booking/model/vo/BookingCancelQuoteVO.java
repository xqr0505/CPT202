package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户取消前的试算结果（不写库，供确认弹窗展示）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户取消预约试算")
public class BookingCancelQuoteVO {

    @Schema(description = "是否允许进入确认取消流程")
    private boolean allowed;

    @Schema(description = "不可取消时的机器可读原因", example = "TOO_CLOSE_TO_START")
    private String reasonCode;

    @Schema(description = "前端展示用说明")
    private String message;

    @Schema(description = "命中的规则档", example = "FULL_REFUND")
    private String policyType;

    @Schema(description = "预约时段开始时间")
    private LocalDateTime bookingStartAt;

    @Schema(description = "作为计算基数的订单金额（咨询费）")
    private BigDecimal orderAmount;

    @Schema(description = "预估可退金额")
    private BigDecimal refundAmount;

    @Schema(description = "预估违约金")
    private BigDecimal penaltyAmount;
}

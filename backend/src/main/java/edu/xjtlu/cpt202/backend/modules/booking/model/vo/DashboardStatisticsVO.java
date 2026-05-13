package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer dashboard statistics view object.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
@Data
@Schema(description = "Customer dashboard statistics view object")
public class DashboardStatisticsVO {

    @Schema(description = "Total completed appointments", example = "12")
    private Integer totalCompletedAppointments = 0;

    @Schema(description = "Total amount spent", example = "1680.00")
    private BigDecimal totalAmountSpent = BigDecimal.ZERO;

    @Schema(description = "Total consultation hours", example = "6.5")
    private Double totalConsultationHours = 0.0D;

    @Schema(description = "Distinct specialists consulted by current customer")
    private List<ConsultedExpertVO> consultedExperts = new ArrayList<>();

    @Schema(description = "Trend chart data")
    private List<TrendChartVO> trendData = new ArrayList<>();

    @Schema(description = "Category distribution chart data")
    private List<CategoryChartVO> categoryData = new ArrayList<>();

    @Schema(description = "Consultation habit chart data")
    private List<HabitChartVO> habitData = new ArrayList<>();

    @Data
    @Schema(description = "Consulted specialist item")
    public static class ConsultedExpertVO {

        @Schema(description = "Specialist ID", example = "1001")
        private Long specialistId;

        @Schema(description = "Specialist full name", example = "Dr. Emily Chen")
        private String specialistName;

        @Schema(description = "Specialist avatar URL", example = "https://cdn.example.com/avatars/specialist-1001.jpg")
        private String specialistAvatar;
    }

    @Data
    @Schema(description = "Trend chart item")
    public static class TrendChartVO {

        @Schema(description = "X-axis date label", example = "2026-04")
        private String dateLabel;

        @Schema(description = "Completed appointment count", example = "6")
        private Integer count;

        @Schema(description = "Total consultation hours", example = "3.5")
        private Double hours;
    }

    @Data
    @Schema(description = "Category chart item")
    public static class CategoryChartVO {

        @Schema(description = "Category name", example = "Psychology")
        private String categoryName;

        @Schema(description = "Total amount spent for this category", example = "520.00")
        private BigDecimal amount;

        @Schema(description = "Completed appointment count for this category", example = "4")
        private Integer count;
    }

    @Data
    @Schema(description = "Habit chart item")
    public static class HabitChartVO {

        @Schema(description = "Day of week label", example = "Mon")
        private String dayOfWeek;

        @Schema(description = "Completed appointment count", example = "3")
        private Integer count;
    }
}

package edu.xjtlu.cpt202.backend.modules.booking.constant;

import java.util.List;

/**
 * Constants for customer dashboard statistics.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
public final class DashboardConstant {

    private DashboardConstant() {
    }

    public static final int TREND_DAILY_THRESHOLD_DAYS = 31;

    public static final String WEEKDAY_MON = "Mon";
    public static final String WEEKDAY_TUE = "Tue";
    public static final String WEEKDAY_WED = "Wed";
    public static final String WEEKDAY_THU = "Thu";
    public static final String WEEKDAY_FRI = "Fri";
    public static final String WEEKDAY_SAT = "Sat";
    public static final String WEEKDAY_SUN = "Sun";

    public static final List<String> WEEKDAY_ORDER = List.of(
            WEEKDAY_MON,
            WEEKDAY_TUE,
            WEEKDAY_WED,
            WEEKDAY_THU,
            WEEKDAY_FRI,
            WEEKDAY_SAT,
            WEEKDAY_SUN
    );

}

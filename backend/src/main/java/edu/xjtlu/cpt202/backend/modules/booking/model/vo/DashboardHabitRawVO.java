package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.Data;

/**
 * Raw habit aggregation item for mapper result.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
@Data
public class DashboardHabitRawVO {

    private Integer dayOfWeek;

    private Integer count;
}

package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 *
 * @author QiranXiao
 * @since 2026/4/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBookingSearchResultVO {

    private long totalMatched;

    private int returnedCount;

    private List<AiBookingSearchItemVO> items;
}

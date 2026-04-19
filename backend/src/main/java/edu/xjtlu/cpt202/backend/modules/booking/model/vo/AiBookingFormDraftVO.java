package edu.xjtlu.cpt202.backend.modules.booking.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Lightweight booking form draft used by AI tools.
 *
 * @author QiranXiao
 * @since 2026/4/18
 */
@Data
@Builder
public class AiBookingFormDraftVO {

    private Long customerId;

    private Long specialistId;

    private Long slotId;

    private String topic;

    private String customerNotes;

    private List<String> availableTopics;

    private List<String> warnings;
}

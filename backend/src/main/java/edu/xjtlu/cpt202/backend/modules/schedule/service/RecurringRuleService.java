package edu.xjtlu.cpt202.backend.modules.schedule.service;

import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateRecurringRuleRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;

import java.util.List;

/**
 * Service interface for recurring availability rule management.
 * @author Schedule Module Team
 */
public interface RecurringRuleService {

    /**
     * Create a new recurring availability rule for the current specialist.
     * @param request the create recurring rule request
     * @return the created recurring rule VO
     */
    RecurringRuleVO createRecurringRule(CreateRecurringRuleRequest request);

    /**
     * Get all recurring rules for the current specialist.
     * @return list of recurring rule VOs
     */
    List<RecurringRuleVO> getAllRecurringRules();

    /**
     * Get active recurring rules for the current specialist.
     * @return list of active recurring rule VOs
     */
    List<RecurringRuleVO> getActiveRecurringRules();

    /**
     * Delete a recurring rule by ID.
     * @param ruleId the rule ID
     */
    void deleteRecurringRule(Long ruleId);
}

package edu.xjtlu.cpt202.backend.modules.schedule.service;

import edu.xjtlu.cpt202.backend.modules.schedule.model.dto.CreateRecurringRuleRequest;
import edu.xjtlu.cpt202.backend.modules.schedule.model.vo.RecurringRuleVO;

import java.util.List;

public interface RecurringRuleService {

    RecurringRuleVO createRecurringRule(CreateRecurringRuleRequest request);

    List<RecurringRuleVO> getAllRecurringRules();

    List<RecurringRuleVO> getActiveRecurringRules();

    void deleteRecurringRule(Long ruleId);
}

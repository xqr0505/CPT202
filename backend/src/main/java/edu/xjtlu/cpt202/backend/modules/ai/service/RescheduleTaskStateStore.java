package edu.xjtlu.cpt202.backend.modules.ai.service;

import edu.xjtlu.cpt202.backend.modules.ai.model.RescheduleTaskState;

import java.util.Optional;

/**
 * Storage abstraction for reschedule workflow task state.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
public interface RescheduleTaskStateStore {

    Optional<RescheduleTaskState> get(Long userId);

    void save(Long userId, RescheduleTaskState state);

    void clear(Long userId);
}

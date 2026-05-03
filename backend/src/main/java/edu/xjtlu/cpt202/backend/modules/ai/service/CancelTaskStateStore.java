package edu.xjtlu.cpt202.backend.modules.ai.service;

import edu.xjtlu.cpt202.backend.modules.ai.model.CancelTaskState;

import java.util.Optional;

/**
 * Persistence abstraction for cancel workflow state.
 *
 * @author QiranXiao
 * @since 2026/5/4
 */
public interface CancelTaskStateStore {

    Optional<CancelTaskState> get(Long userId);

    void save(Long userId, CancelTaskState state);

    void clear(Long userId);
}

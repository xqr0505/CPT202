package edu.xjtlu.cpt202.backend.modules.ai.service;

import edu.xjtlu.cpt202.backend.modules.ai.model.BookingTaskState;

import java.util.Optional;

public interface BookingTaskStateStore {

    Optional<BookingTaskState> get(Long userId);

    void save(Long userId, BookingTaskState state);

    void clear(Long userId);
}

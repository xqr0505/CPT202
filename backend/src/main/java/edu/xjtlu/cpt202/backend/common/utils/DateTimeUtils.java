package edu.xjtlu.cpt202.backend.common.utils;

import java.time.LocalDateTime;
/**
 * @author QiranXiao
 * @date 2026/3/26
 */
public class DateTimeUtils {

    private DateTimeUtils() {
    }

    /**
     * check if time periods overlap
     */
    public static boolean isOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}


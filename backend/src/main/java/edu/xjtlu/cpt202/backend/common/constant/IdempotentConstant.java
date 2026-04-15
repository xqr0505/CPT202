package edu.xjtlu.cpt202.backend.common.constant;

/**
 * Constants for idempotent request protection.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
public final class IdempotentConstant {

    private IdempotentConstant() {
    }

    public static final Long ANONYMOUS_USER_ID = -1L;

    public static final String IDEMPOTENT_PREFIX = "idempotent";
    public static final String IDEMPOTENT_VALUE = "1";

    public static final String KEY_SEPARATOR = ":";
}

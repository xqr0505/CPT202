package edu.xjtlu.cpt202.backend.common.constant;

/**
 * Redis cache constants for key namespace and ttl.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
public final class RedisCacheConstant {

    private RedisCacheConstant() {
    }

    public static final String KEY_SEPARATOR = ":";
    public static final String KEY_PATTERN_WILDCARD = "*";

    public static final String BOOKING_CACHE_PREFIX = "booking";
    public static final String CUSTOMER_CACHE_PREFIX = "customer";
    public static final String LIST_CACHE_PREFIX = "list";
    public static final String DETAIL_CACHE_PREFIX = "detail";
    public static final String CACHE_DIMENSION_ALL = "ALL";

    public static final long BOOKING_LIST_CACHE_TTL_SECONDS = 300L;
    public static final long BOOKING_DETAIL_CACHE_TTL_SECONDS = 600L;
}

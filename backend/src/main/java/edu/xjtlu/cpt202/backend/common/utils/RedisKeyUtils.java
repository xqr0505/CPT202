package edu.xjtlu.cpt202.backend.common.utils;

import edu.xjtlu.cpt202.backend.common.constant.RedisCacheConstant;

import java.util.Optional;

/**
 * Redis key builder utilities.
 *
 * @author QiranXiao
 * @since 2026/4/14
 */
public final class RedisKeyUtils {

    private RedisKeyUtils() {
    }

    public static String buildCustomerBookingListKey(Long customerId, Integer pageNo, Integer pageSize, String tab, String status) {
        return String.join(
                RedisCacheConstant.KEY_SEPARATOR,
                RedisCacheConstant.BOOKING_CACHE_PREFIX,
                RedisCacheConstant.CUSTOMER_CACHE_PREFIX,
                String.valueOf(customerId),
                RedisCacheConstant.LIST_CACHE_PREFIX,
                String.valueOf(Optional.ofNullable(pageNo).orElse(0)),
                String.valueOf(Optional.ofNullable(pageSize).orElse(0)),
                normalizeDimension(tab),
                normalizeDimension(status)
        );
    }

    public static String buildCustomerBookingDetailKey(Long customerId, Long bookingId) {
        return String.join(
                RedisCacheConstant.KEY_SEPARATOR,
                RedisCacheConstant.BOOKING_CACHE_PREFIX,
                RedisCacheConstant.CUSTOMER_CACHE_PREFIX,
                String.valueOf(customerId),
                RedisCacheConstant.DETAIL_CACHE_PREFIX,
                String.valueOf(bookingId)
        );
    }

    public static String buildCustomerBookingKeyPattern(Long customerId) {
        return String.join(
                RedisCacheConstant.KEY_SEPARATOR,
                RedisCacheConstant.BOOKING_CACHE_PREFIX,
                RedisCacheConstant.CUSTOMER_CACHE_PREFIX,
                String.valueOf(customerId),
                RedisCacheConstant.KEY_PATTERN_WILDCARD
        );
    }

    private static String normalizeDimension(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .orElse(RedisCacheConstant.CACHE_DIMENSION_ALL);
    }
}

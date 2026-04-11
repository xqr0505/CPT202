package edu.xjtlu.cpt202.backend.common.utils;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Quickly obtain the ID, role and authentication status of the currently logged-in user,
 * and clear these information when necessary.
 *
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class SecurityUtils {

    private static final Long DEV_SPECIALIST_ID = 1L;

    public static Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            return userId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
            if (principal instanceof Number) {
                return ((Number) principal).longValue();
            }
            if (principal instanceof String) {
                try {
                    return Long.valueOf((String) principal);
                } catch (NumberFormatException ignored) {}
            }
        }
        // 无法获取用户ID → 抛出未认证异常
        throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), 
            "User not authenticated. Please login.");
    }

    public static String getCurrentUserRole() {
        String role = UserContextHolder.getRole();
        if (role != null) {
            return role;
        }
        throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), 
            "User role not found. Please login.");
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    public static void clearContext() {
        SecurityContextHolder.clearContext();
        UserContextHolder.clear();
    }
}

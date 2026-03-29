package edu.xjtlu.cpt202.backend.common.utils;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 工具类 - 对外提供统一的用户上下文访问接口
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 从 Authentication 的 principal 中获取 userId
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户的角色
     */
    public static String getCurrentUserRole() {
        return UserContextHolder.getRole();
    }

    /**
     * 获取当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 清除当前用户的安全上下文
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
        UserContextHolder.clear();
    }
}

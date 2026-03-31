package edu.xjtlu.cpt202.backend.common.utils;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Quickly obtain the ID, role and authentication status of the currently logged-in user, 
 * and clear these information when necessary.
 * @author DanyiHuang
 * @date 2026/3/29
 */
public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                return (Long) principal;
            }
        }
        return null;
    }

    public static String getCurrentUserRole() {
        return UserContextHolder.getRole();
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

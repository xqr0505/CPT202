package edu.xjtlu.cpt202.backend.common.utils;

import edu.xjtlu.cpt202.backend.common.context.UserContextHolder;
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
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long principalUserId) {
                return principalUserId;
            }
            if (principal instanceof Number principalNumber) {
                return principalNumber.longValue();
            }
            if (principal instanceof String principalText
                    && !principalText.isBlank()
                    && !"anonymousUser".equals(principalText)) {
                try {
                    return Long.valueOf(principalText);
                } catch (NumberFormatException ignored) {
                    // Fall through to the development fallback below.
                }
            }
        }

        return DEV_SPECIALIST_ID;
    }

    public static String getCurrentUserRole() {
        String role = UserContextHolder.getRole();
        return role != null ? role : "SPECIALIST";
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

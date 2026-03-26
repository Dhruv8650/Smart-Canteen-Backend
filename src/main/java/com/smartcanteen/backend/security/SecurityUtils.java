package com.smartcanteen.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // EMAIL FETCH
    public static String getCurrentUserEmail() {

        Authentication auth = getAuth();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        return auth.getName();
    }

    // ROLE FETCH
    public static String getCurrentUserRole() {

        Authentication auth = getAuth();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        return auth.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    // ADMIN CHECK
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    // MANAGER CHECK
    public static boolean isManager() {
        return "MANAGER".equals(getCurrentUserRole());
    }

    // KITCHEN CHECK
    public static boolean isKitchen() {
        return "KITCHEN".equals(getCurrentUserRole());
    }
}
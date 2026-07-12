package com.hust.commonlibrary.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public class SecurityUtils {

    /**
     * Lấy UserId (Subject) từ SecurityContext của Spring Security.
     * @return Optional chứa UserId hoặc Empty nếu chưa đăng nhập.
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    /**
     * Lấy UserId và ném lỗi nếu không tìm thấy.
     */
    public static String getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    /**
     * Lấy đối tượng Jwt gốc từ SecurityContext.
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /**
     * Kiểm tra xem User hiện tại có sở hữu một trong các role được chỉ định không.
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (String role : roles) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
            if (hasRole) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem User hiện tại có phải là ADMIN không.
     */
    public static boolean isAdmin() {
        return hasAnyRole("ADMIN");
    }
}

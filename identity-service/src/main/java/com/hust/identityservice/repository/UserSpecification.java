package com.hust.identityservice.repository;

import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasSearchQuery(String q) {
        return (root, query, cb) -> {
            if (q == null || q.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> {
            if (role == null || role.trim().isEmpty()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("role")), role.trim().toLowerCase());
        };
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }
}

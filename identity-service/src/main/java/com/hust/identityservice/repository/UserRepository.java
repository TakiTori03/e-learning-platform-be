package com.hust.identityservice.repository;

import com.hust.identityservice.entity.User;
import com.hust.identityservice.entity.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import com.hust.identityservice.repository.projection.SignupProjection;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Boolean existsByEmail(String email);
    java.util.Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
    List<User> findByRole(String role);

    // Báo cáo User đăng ký mới theo tháng (Native Query PostgreSQL)
    @Query(
        value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as period, COUNT(id) as count " +
                "FROM users GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY period", 
        nativeQuery = true
    )
    List<SignupProjection> getNewSignupsByMonth();

    @Query(
        value = "SELECT TO_CHAR(created_at, :format) as period, COUNT(id) as count " +
                "FROM users " +
                "WHERE role = 'STUDENT' " +
                "  AND (CAST(:startDate AS VARCHAR) IS NULL OR CAST(:startDate AS VARCHAR) = '' OR created_at >= CAST(:startDate AS TIMESTAMP)) " +
                "  AND (CAST(:endDate AS VARCHAR) IS NULL OR CAST(:endDate AS VARCHAR) = '' OR created_at <= CAST(:endDate AS TIMESTAMP)) " +
                "GROUP BY 1 " +
                "ORDER BY period", 
        nativeQuery = true
    )
    List<SignupProjection> getNewSignupsDynamic(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("format") String format
    );

    // Lọc các User VIP hoặc Active tích cực nhất (sắp xếp theo last_login)
    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.lastLogin DESC")
    org.springframework.data.domain.Page<User> getTopUsersByLastLogin(
        @Param("status") UserStatus status, 
        org.springframework.data.domain.Pageable pageable
    );

}



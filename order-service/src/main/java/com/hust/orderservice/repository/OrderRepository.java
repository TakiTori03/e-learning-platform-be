package com.hust.orderservice.repository;


import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.hust.orderservice.repository.projection.RevenueProjection;
import com.hust.orderservice.repository.projection.CourseSalesProjection;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    org.springframework.data.domain.Page<Order> findByUserId(String userId, org.springframework.data.domain.Pageable pageable);
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    @Query("SELECT i.courseId, COUNT(o) FROM Order o JOIN o.items i WHERE i.courseId IN :courseIds AND o.status = :status GROUP BY i.courseId")
    List<Object[]> countByCourseIdsAndStatus(@Param("courseIds") List<String> courseIds, @Param("status") OrderStatus status);

    boolean existsByUserIdAndStatusAndItems_CourseId(String userId, OrderStatus status, String courseId);

    @Query("SELECT DISTINCT i.courseId FROM Order o JOIN o.items i WHERE o.userId = :userId AND o.status = :status AND i.courseId IN :courseIds")
    List<String> findBoughtCourseIds(@Param("userId") String userId, @Param("courseIds") List<String> courseIds, @Param("status") OrderStatus status);

    // Báo cáo doanh thu theo tháng (Native Query PostgreSQL)
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as period, SUM(total_price) as revenue " +
                   "FROM orders WHERE status = :status GROUP BY TO_CHAR(created_at, 'YYYY-MM') ORDER BY period", 
           nativeQuery = true)
    List<RevenueProjection> getRevenuesByMonth(@Param("status") String status);

    @Query(value = "SELECT TO_CHAR(created_at, :format) as period, SUM(total_price) as revenue " +
                   "FROM orders " +
                   "WHERE status = :status " +
                   "  AND (CAST(:startDate AS VARCHAR) IS NULL OR CAST(:startDate AS VARCHAR) = '' OR created_at >= CAST(:startDate AS TIMESTAMP)) " +
                   "  AND (CAST(:endDate AS VARCHAR) IS NULL OR CAST(:endDate AS VARCHAR) = '' OR created_at <= CAST(:endDate AS TIMESTAMP)) " +
                   "GROUP BY 1 " +
                   "ORDER BY period", 
           nativeQuery = true)
    List<RevenueProjection> getRevenuesDynamic(
        @Param("status") String status,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("format") String format
    );

    // Báo cáo doanh số bán khóa học (Native Query PostgreSQL)
    @Query(value = "SELECT oi.course_id as courseId, oi.name as courseName, COUNT(o.id) as sales, SUM(oi.final_price) as revenue " +
                   "FROM orders o JOIN order_items oi ON o.id = oi.order_id " +
                   "WHERE o.status = :status GROUP BY oi.course_id, oi.name ORDER BY sales DESC",
           nativeQuery = true)
    List<CourseSalesProjection> getCourseSales(@Param("status") String status);

    // Lọc các đơn hàng giá trị cao nhất
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.totalPrice DESC")
    org.springframework.data.domain.Page<Order> getTopValueOrders(@Param("status") OrderStatus status, org.springframework.data.domain.Pageable pageable);
}

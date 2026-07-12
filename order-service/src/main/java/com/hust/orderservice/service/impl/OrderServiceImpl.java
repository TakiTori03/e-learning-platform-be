package com.hust.orderservice.service.impl;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.client.CourseClient;
import com.hust.orderservice.client.dto.CourseInternalResponse;
import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.dto.request.OrderRequest;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.entity.OrderItem;
import com.hust.orderservice.mapper.OrderMapper;
import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.service.OrderService;
import org.springframework.context.ApplicationEventPublisher;
import com.hust.orderservice.dto.event.OrderPaidInternalEvent;
import com.hust.commonlibrary.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hust.commonlibrary.annotation.DistributedLock;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CourseClient courseClient;
    private final ApplicationEventPublisher eventPublisher;
    private final com.hust.commonlibrary.service.RedisService redisService;

    private void populateUserProfile(OrderResponse response) {
        if (response == null || response.getUserId() == null) return;
        try {
            String key = com.hust.commonlibrary.constants.RedisPrefixConstants.getSharedUserProfileKey(response.getUserId());
            com.hust.commonlibrary.dto.UserSharedProfile profile = redisService.get(key, com.hust.commonlibrary.dto.UserSharedProfile.class);
            if (profile != null) {
                response.setUser(profile);
            }
        } catch (Exception e) {
            log.error("Failed to load user shared profile from Redis for user {}: {}", response.getUserId(), e.getMessage());
        }
    }

    private void populateUserProfiles(List<OrderResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        responses.forEach(this::populateUserProfile);
    }

    @Override
    @Transactional
    @DistributedLock(key = "'order:create:' + #request.userId", waitTime = 5, leaseTime = 15) // 🔒 100% An toàn: Khóa luồng xử lý theo UserID!
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user id: {}", request.getUserId());

        // Loại bỏ courseIds trùng lặp
        List<String> distinctCourseIds = request.getCourseIds().stream().distinct().toList();

        // Chặn mua trùng lặp nếu người dùng đã sở hữu khóa học (hoặc đang xử lý thanh toán thành công)
        for (String courseId : distinctCourseIds) {
            if (checkIfBought(request.getUserId(), courseId)) {
                log.warn("User {} already purchased or is processing course {}", request.getUserId(), courseId);
                throw new IllegalArgumentException("Bạn đã sở hữu khóa học này hoặc đơn hàng đang được xử lý!");
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        // 1. Fetch course details and calculate total
        for (String courseId : distinctCourseIds) {
            ApiResponse<CourseInternalResponse> response = courseClient.getCourseDetail(courseId);

            if (response == null || !response.isSuccess() || response.getPayload() == null) {
                log.error("Course not found or service error: {}", courseId);
                throw new RuntimeException("Course not found: " + courseId);
            }

            CourseInternalResponse course = response.getPayload();

            OrderItem item = OrderItem.builder()
                    .courseId(course.getId())
                    .name(course.getName())
                    .finalPrice(BigDecimal.valueOf(course.getFinalPrice()))
                    .thumbnail(course.getThumbnail())
                    .build();

            orderItems.add(item);
            originalTotal = originalTotal.add(item.getFinalPrice());
        }

        // 2. Build and save Order (No Coupon logic)
        Order order = orderMapper.requestToEntity(request);
        order.setItems(orderItems);
        order.setTotalPrice(originalTotal);
        order.setVatFee(BigDecimal.ZERO);

        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.PAYMENT_SUCCESS);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}, Total: {}", savedOrder.getId(), originalTotal);

        // NẾU LÀ ĐƠN HÀNG 0 ĐỒNG -> Bắn sự kiện ngay lập tức để mở khóa học
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Free order detected, publishing enrollment event immediately for Order: {}", savedOrder.getId());
            OrderPaidEvent event = OrderPaidEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .courseIds(distinctCourseIds)
                    .build();
            eventPublisher.publishEvent(new OrderPaidInternalEvent(this, event));
        }

        OrderResponse response = orderMapper.entityToResponse(savedOrder);
        populateUserProfile(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        OrderResponse response = orderMapper.entityToResponse(order);
        populateUserProfile(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public com.hust.commonlibrary.dto.ListResponse<OrderResponse> getOrdersByUserId(String userId, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Order> page = orderRepository.findByUserId(userId, pageable);
        List<OrderResponse> content = page.stream()
                .map(orderMapper::entityToResponse)
                .toList();
        populateUserProfiles(content);
        return com.hust.commonlibrary.dto.ListResponse.of(content, page);
    }

    @Override
    public java.util.Map<String, Long> getEnrollmentCountsBulk(List<String> courseIds) {
        List<Object[]> results = orderRepository.countByCourseIdsAndStatus(courseIds, OrderStatus.COMPLETED);
        java.util.Map<String, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        res -> (String) res[0],
                        res -> (Long) res[1]
                ));

        courseIds.forEach(id -> countMap.putIfAbsent(id, 0L));
        return countMap;
    }

    @Override
    public boolean checkIfBought(String userId, String courseId) {
        return orderRepository.existsByUserIdAndStatusAndItems_CourseId(userId, OrderStatus.COMPLETED, courseId);
    }

    @Override
    public Map<String, Boolean> checkIfBoughtBulk(String userId, List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return Map.of();

        List<String> boughtIds = orderRepository.findBoughtCourseIds(userId, courseIds, OrderStatus.COMPLETED);
        Set<String> boughtSet = new HashSet<>(boughtIds);

        return courseIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        boughtSet::contains
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public com.hust.commonlibrary.dto.ListResponse<OrderResponse> getAllOrders(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Order> page = orderRepository.findAll(pageable);
        List<OrderResponse> content = page.stream()
                .map(orderMapper::entityToResponse)
                .toList();
        populateUserProfiles(content);
        return com.hust.commonlibrary.dto.ListResponse.of(content, page);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);

        Order savedOrder = orderRepository.save(order);
        log.info("Admin manually updated Order {} status to {}", orderId, newStatus);

        // NẾU TỪ CHỜ DUYỆT -> THÀNH CÔNG, HÃY KÍCH HOẠT ĐĂNG KÝ KHÓA HỌC
        if (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.PAYMENT_SUCCESS) {
            List<String> courseIds = order.getItems().stream().map(OrderItem::getCourseId).toList();
            log.info("Triggering course enrollment manual event for Order {}", orderId);
            OrderPaidEvent event = OrderPaidEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .courseIds(courseIds)
                    .build();
            eventPublisher.publishEvent(new OrderPaidInternalEvent(this, event));
        }

        OrderResponse response = orderMapper.entityToResponse(savedOrder);
        populateUserProfile(response);
        return response;
    }
}

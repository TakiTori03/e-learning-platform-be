package com.hust.orderservice.service.impl;

import com.hust.orderservice.entity.Cart;
import com.hust.orderservice.repository.CartRepository;
import com.hust.orderservice.service.CartService;
import com.hust.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final OrderService orderService;

    @Override
    public Set<String> getCart(String userId) {
        log.info("Getting cart for User ID: {}", userId);
        Cart cart = cartRepository.findById(userId).orElse(null);
        if (cart == null) {
            return new HashSet<>();
        }
        
        // Loại bỏ các khóa học đã mua khỏi giỏ hàng
        boolean removed = cart.getCourseIds().removeIf(cid -> orderService.checkIfBought(userId, cid));
        if (removed) {
            cartRepository.save(cart);
        }
        
        return cart.getCourseIds();
    }

    @Override
    public Set<String> syncCart(String userId, Set<String> localCourseIds) {
        log.info("Syncing cart for User ID: {} with local ids: {}", userId, localCourseIds);
        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> Cart.builder().userId(userId).courseIds(new HashSet<>()).build());

        if (localCourseIds != null && !localCourseIds.isEmpty()) {
            for (String cid : localCourseIds) {
                if (!orderService.checkIfBought(userId, cid)) {
                    cart.getCourseIds().add(cid);
                }
            }
        }

        // Dọn dẹp cả giỏ hàng cũ trên DB nếu có khóa học nào đã mua
        cart.getCourseIds().removeIf(cid -> orderService.checkIfBought(userId, cid));

        cartRepository.save(cart);
        return cart.getCourseIds();
    }

    @Override
    public Set<String> addCourseToCart(String userId, String courseId) {
        log.info("Adding course {} to cart for User ID: {}", courseId, userId);
        if (orderService.checkIfBought(userId, courseId)) {
            log.warn("User {} tried to add already purchased course {} to cart", userId, courseId);
            throw new IllegalArgumentException("Bạn đã sở hữu khóa học này rồi!");
        }

        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> Cart.builder().userId(userId).courseIds(new HashSet<>()).build());

        cart.getCourseIds().add(courseId);
        cartRepository.save(cart);
        return cart.getCourseIds();
    }

    @Override
    public Set<String> removeCourseFromCart(String userId, String courseId) {
        log.info("Removing course {} from cart for User ID: {}", courseId, userId);
        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> Cart.builder().userId(userId).courseIds(new HashSet<>()).build());

        cart.getCourseIds().remove(courseId);
        cartRepository.save(cart);
        return cart.getCourseIds();
    }

    @Override
    public void clearCart(String userId) {
        log.info("Clearing cart for User ID: {}", userId);
        cartRepository.findById(userId).ifPresent(cart -> {
            cart.getCourseIds().clear();
            cartRepository.save(cart);
        });
    }
}

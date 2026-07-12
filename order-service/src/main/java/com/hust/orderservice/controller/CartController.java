package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.orderservice.dto.request.CartSyncRequest;
import com.hust.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<Set<String>>> getCart() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to get Cart for UserID: {}", userId);
        return ResponseEntity.ok(
                ApiResponse.<Set<String>>builder()
                        .success(true)
                        .payload(cartService.getCart(userId))
                        .build()
        );
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Set<String>>> syncCart(@RequestBody CartSyncRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to sync Cart for UserID: {} with size: {}", userId,
                request.getCourseIds() != null ? request.getCourseIds().size() : 0);
        return ResponseEntity.ok(
                ApiResponse.<Set<String>>builder()
                        .success(true)
                        .payload(cartService.syncCart(userId, request.getCourseIds()))
                        .build()
        );
    }

    @PostMapping("/add/{courseId}")
    public ResponseEntity<ApiResponse<Set<String>>> addCourseToCart(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to add course {} to Cart for UserID: {}", courseId, userId);
        return ResponseEntity.ok(
                ApiResponse.<Set<String>>builder()
                        .success(true)
                        .payload(cartService.addCourseToCart(userId, courseId))
                        .build()
        );
    }

    @DeleteMapping("/remove/{courseId}")
    public ResponseEntity<ApiResponse<Set<String>>> removeCourseFromCart(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to remove course {} from Cart for UserID: {}", courseId, userId);
        return ResponseEntity.ok(
                ApiResponse.<Set<String>>builder()
                        .success(true)
                        .payload(cartService.removeCourseFromCart(userId, courseId))
                        .build()
        );
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("REST request to clear Cart for UserID: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .build()
        );
    }
}

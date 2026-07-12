package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.interactionservice.dto.request.WishlistRequest;
import com.hust.interactionservice.dto.response.WishlistResponse;
import com.hust.interactionservice.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(@RequestBody @Valid WishlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<WishlistResponse>builder()
                .success(true)
                .payload(wishlistService.addToWishlist(request))
                .build());
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable String courseId) {
        wishlistService.removeFromWishlist(courseId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Removed from wishlist")
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getMyWishlist() {
        return ResponseEntity.ok(ApiResponse.<List<WishlistResponse>>builder()
                .success(true)
                .payload(wishlistService.getUserWishlist())
                .build());
    }

    @GetMapping("/status/{courseId}")
    public ResponseEntity<ApiResponse<Boolean>> checkStatus(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .payload(wishlistService.checkStatus(courseId))
                .build());
    }
}

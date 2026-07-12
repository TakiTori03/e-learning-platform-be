package com.hust.interactionservice.service.impl;

import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.interactionservice.dto.request.WishlistRequest;
import com.hust.interactionservice.dto.response.WishlistResponse;
import com.hust.interactionservice.entity.Wishlist;
import com.hust.interactionservice.mapper.WishlistMapper;
import com.hust.interactionservice.repository.WishlistRepository;
import com.hust.interactionservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public WishlistResponse addToWishlist(WishlistRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        // Kiểm tra xem đã tồn tại trong wishlist chưa
        return wishlistRepository.findByUserIdAndCourseId(userId, request.getCourseId())
                .map(wishlistMapper::entityToResponse)
                .orElseGet(() -> {
                    Wishlist wishlist = Wishlist.builder()
                            .userId(userId)
                            .courseId(request.getCourseId())
                            .build();
                    return wishlistMapper.entityToResponse(wishlistRepository.save(wishlist));
                });
    }

    @Override
    public void removeFromWishlist(String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        wishlistRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(wishlistRepository::delete);
    }

    @Override
    public List<WishlistResponse> getUserWishlist() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return wishlistRepository.findByUserId(userId).stream()
                .map(wishlistMapper::entityToResponse)
                .toList();
    }

    @Override
    public boolean checkStatus(String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        return wishlistRepository.findByUserIdAndCourseId(userId, courseId).isPresent();
    }

    @Override
    public List<String> getUserWishlistCourseIds(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return List.of();
        }
        return wishlistRepository.findByUserId(userId).stream()
                .map(Wishlist::getCourseId)
                .toList();
    }
}

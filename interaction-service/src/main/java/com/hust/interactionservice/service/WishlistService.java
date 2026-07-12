package com.hust.interactionservice.service;

import com.hust.interactionservice.dto.request.WishlistRequest;
import com.hust.interactionservice.dto.response.WishlistResponse;
import java.util.List;

public interface WishlistService {
    WishlistResponse addToWishlist(WishlistRequest request);
    void removeFromWishlist(String courseId);
    List<WishlistResponse> getUserWishlist();
    boolean checkStatus(String courseId);
    List<String> getUserWishlistCourseIds(String userId);
}

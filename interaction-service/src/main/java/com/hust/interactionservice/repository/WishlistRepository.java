package com.hust.interactionservice.repository;

import com.hust.interactionservice.entity.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {
    Optional<Wishlist> findByUserIdAndCourseId(String userId, String courseId);
    List<Wishlist> findByUserId(String userId);
}

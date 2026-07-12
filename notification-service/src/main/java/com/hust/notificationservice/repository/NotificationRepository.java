package com.hust.notificationservice.repository;

import com.hust.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findAllByUserIdInOrderByCreatedAtDesc(List<String> userIds, Pageable pageable);
    long countByUserIdInAndIsReadFalse(List<String> userIds);
    List<Notification> findAllByUserIdInAndIsReadFalse(List<String> userIds);
}

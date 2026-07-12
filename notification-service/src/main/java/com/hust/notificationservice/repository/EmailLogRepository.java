package com.hust.notificationservice.repository;

import com.hust.notificationservice.entity.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
    List<EmailLog> findAllByRecipientEmail(String recipientEmail);
}

package com.hust.notificationservice.service;

import java.util.List;
import java.util.Map;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String templateName, Object templateModel);
    void sendBulkHtmlEmails(List<String> toList, String subject, String templateName, Map<String, Object> baseModel);
}

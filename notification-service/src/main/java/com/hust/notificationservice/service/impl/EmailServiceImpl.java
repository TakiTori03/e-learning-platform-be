package com.hust.notificationservice.service.impl;

import com.hust.notificationservice.entity.EmailLog;
import com.hust.notificationservice.entity.enums.EmailStatus;
import com.hust.notificationservice.repository.EmailLogRepository;
import com.hust.notificationservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Object templateModel) {
        log.info("Starting async email sending to: {}", to);
        EmailLog emailLog = EmailLog.builder()
                .recipientEmail(to)
                .subject(subject)
                .build();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            if (templateModel instanceof Map) {
                context.setVariables((Map<String, Object>) templateModel);
            }

            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            emailLog.setStatus(EmailStatus.SENT);
            log.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
        } finally {
            emailLogRepository.save(emailLog);
        }
    }

    @Override
    @Async
    public void sendBulkHtmlEmails(java.util.List<String> toList, String subject, String templateName, Map<String, Object> baseModel) {
        log.info("Starting async bulk email sending to {} recipients", toList.size());
        java.util.List<MimeMessage> messages = new java.util.ArrayList<>();
        java.util.List<EmailLog> logs = new java.util.ArrayList<>();

        for (String to : toList) {
            EmailLog emailLog = EmailLog.builder()
                    .recipientEmail(to)
                    .subject(subject)
                    .build();
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(
                        message,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name()
                );

                Context context = new Context();
                context.setVariables(baseModel);
                // Add recipient-specific variables
                context.setVariable("unsubscribeUrl", "http://localhost:8080/notification/subscriptions/" + to);

                String htmlContent = templateEngine.process(templateName, context);

                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                messages.add(message);
                emailLog.setStatus(EmailStatus.SENT);
                logs.add(emailLog);
            } catch (Exception e) {
                log.error("Failed to prepare bulk email for: {}", to, e);
                emailLog.setStatus(EmailStatus.FAILED);
                emailLog.setErrorMessage(e.getMessage());
                logs.add(emailLog);
            }
        }

        try {
            // Send all MimeMessages in a single connection batch
            if (!messages.isEmpty()) {
                mailSender.send(messages.toArray(new MimeMessage[0]));
                log.info("Successfully dispatched {} bulk emails.", messages.size());
            }
        } catch (Exception e) {
            log.error("Failed to send bulk emails array.", e);
            for (EmailLog logItem : logs) {
                if (logItem.getStatus() == EmailStatus.SENT) {
                    logItem.setStatus(EmailStatus.FAILED);
                    logItem.setErrorMessage("Bulk send failure: " + e.getMessage());
                }
            }
        } finally {
            emailLogRepository.saveAll(logs);
        }
    }
}

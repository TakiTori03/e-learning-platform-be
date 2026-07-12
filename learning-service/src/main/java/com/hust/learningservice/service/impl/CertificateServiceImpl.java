package com.hust.learningservice.service.impl;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.CourseInternalResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.event.CertificateGeneratedEvent;
import com.hust.commonlibrary.event.CertificateIssuedEvent;
import com.hust.learningservice.client.CourseClient;
import com.hust.learningservice.entity.Certificate;
import com.hust.learningservice.entity.CertificateStatus;
import com.hust.learningservice.repository.CertificateRepository;
import com.hust.learningservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseClient courseClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisService redisService;

    @Override
    @Transactional(readOnly = true)
    public Certificate getCertificateByCourse(String userId, String courseId) {
        log.info("Fetching certificate for userId={}, courseId={}", userId, courseId);
        return certificateRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certificate> getMyCertificates(String userId) {
        log.info("Fetching all certificates for userId={}", userId);
        return certificateRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void issueCertificate(String userId, String courseId, Double finalScore) {
        log.info("Initiating certificate issuance for userId={}, courseId={}, score={}", userId, courseId, finalScore);

        // Idempotency check
        if (certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            log.info("Certificate already exists/pending for userId={} and courseId={}. Skipping.", userId, courseId);
            return;
        }

        // 1. Fetch Student Full Name from Shared Redis Cache
        String studentName = "Learner";
        String userCacheKey = RedisPrefixConstants.getSharedUserProfileKey(userId);
        try {
            Object cachedProfile = redisService.get(userCacheKey);
            if (cachedProfile != null) {
                log.info("Found cached user profile in Redis for userId={}", userId);
                UserSharedProfile profile;
                if (cachedProfile instanceof UserSharedProfile p) {
                    profile = p;
                } else {
                    profile = new com.fasterxml.jackson.databind.ObjectMapper()
                            .convertValue(cachedProfile, UserSharedProfile.class);
                }
                String firstName = profile.getFirstName() != null ? profile.getFirstName() : "";
                String lastName = profile.getLastName() != null ? profile.getLastName() : "";
                studentName = (firstName + " " + lastName).trim();
            } else {
                log.warn("🚨 Shared user profile cache miss in Redis for userId={}! Using default placeholder.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch user profile details from Redis. Falling back to placeholder student name. Error: {}", e.getMessage());
        }

        // 2. Fetch Course Details
        String courseName = "Course";
        try {
            ApiResponse<CourseInternalResponse> courseRes = courseClient.getCourseDetail(courseId);
            if (courseRes != null && courseRes.isSuccess() && courseRes.getPayload() != null) {
                courseName = courseRes.getPayload().getName();
            }
        } catch (Exception e) {
            log.error("Failed to fetch course details via Feign. Falling back to placeholder course name. Error: {}", e.getMessage());
        }

        // 3. Calculate classification
        double score = finalScore != null ? finalScore : 0.0;
        String classification;
        if (score >= 90.0) {
            classification = "Xuất sắc";
        } else if (score >= 80.0) {
            classification = "Giỏi";
        } else if (score >= 65.0) {
            classification = "Khá";
        } else {
            classification = "Trung bình";
        }

        // 4. Save Certificate record
        Certificate certificate = Certificate.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .courseId(courseId)
                .courseName(courseName)
                .studentFullName(studentName)
                .finalScore(finalScore)
                .classification(classification)
                .status(CertificateStatus.PENDING)
                .issuedAt(Instant.now())
                .build();

        Certificate saved = certificateRepository.save(certificate);
        log.info("Saved Certificate entity with status PENDING. certId={}, classification={}", saved.getId(), classification);

        // 5. Publish Spring Event (Outbox Pattern)
        CertificateIssuedEvent event = CertificateIssuedEvent.builder()
                .certificateId(saved.getId())
                .userId(userId)
                .courseId(courseId)
                .studentName(studentName)
                .courseName(courseName)
                .finalScore(finalScore)
                .classification(classification)
                .issuedAt(saved.getIssuedAt())
                .build();

        eventPublisher.publishEvent(event);
        log.info("Published local CertificateIssuedEvent for transaction: {}", saved.getId());
    }

    @Override
    @Transactional
    public void handleCertificateGenerated(CertificateGeneratedEvent event) {
        log.info("Processing CertificateGeneratedEvent for certId={}, success={}", event.getCertificateId(), event.getIsSuccess());

        Optional<Certificate> certOpt = certificateRepository.findById(event.getCertificateId());
        if (certOpt.isPresent()) {
            Certificate certificate = certOpt.get();
            if (Boolean.TRUE.equals(event.getIsSuccess())) {
                certificate.setCertificateUrl(event.getCertificateUrl());
                certificate.setStatus(CertificateStatus.COMPLETED);
                log.info("Successfully completed certificate issuance. url={}", event.getCertificateUrl());
            } else {
                certificate.setStatus(CertificateStatus.FAILED);
                log.warn("Certificate generation marked as FAILED by worker-service.");
            }
            certificateRepository.save(certificate);
        } else {
            log.error("Sync Error: Received CertificateGeneratedEvent but certId={} does not exist in learning-service database.", event.getCertificateId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Certificate getCertificateById(String id) {
        log.info("Fetching certificate by id={}", id);
        return certificateRepository.findById(id).orElse(null);
    }
}

package com.hust.learningservice.service;

import com.hust.learningservice.entity.Certificate;
import com.hust.commonlibrary.event.CertificateGeneratedEvent;
import java.util.List;

public interface CertificateService {
    Certificate getCertificateByCourse(String userId, String courseId);
    List<Certificate> getMyCertificates(String userId);
    Certificate getCertificateById(String id);
    void issueCertificate(String userId, String courseId, Double finalScore);
    void handleCertificateGenerated(CertificateGeneratedEvent event);
}

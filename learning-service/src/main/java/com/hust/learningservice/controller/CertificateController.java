package com.hust.learningservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.learningservice.entity.Certificate;
import com.hust.learningservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/course/{courseId}")
    public ApiResponse<Certificate> getCertificateByCourse(@PathVariable String courseId) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Certificate certificate = certificateService.getCertificateByCourse(userId, courseId);
        return ApiResponse.<Certificate>builder()
                .success(true)
                .payload(certificate)
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<Certificate>> getMyCertificates() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<Certificate> certificates = certificateService.getMyCertificates(userId);
        return ApiResponse.<List<Certificate>>builder()
                .success(true)
                .payload(certificates)
                .build();
    }

    @GetMapping("/public/verify/{certificateId}")
    public ApiResponse<Certificate> verifyCertificate(@PathVariable String certificateId) {
        Certificate certificate = certificateService.getCertificateById(certificateId);
        return ApiResponse.<Certificate>builder()
                .success(true)
                .payload(certificate)
                .build();
    }
}

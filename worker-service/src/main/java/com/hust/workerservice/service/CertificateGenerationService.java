package com.hust.workerservice.service;

import com.hust.commonlibrary.event.CertificateIssuedEvent;
import com.hust.workerservice.strategy.StorageStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateGenerationService {

    private final TemplateEngine templateEngine;
    private final StorageStrategy storageStrategy;

    @Value("${app.minio.endpoint}")
    private String minioEndpoint;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private File regularFontFile;
    private File boldFontFile;
    private File italicFontFile;
    private File logoFile;
    private File signatureFile;

    @PostConstruct
    public void initFonts() {
        try {
            regularFontFile = extractResourceToTempFile("/fonts/TimesNewRoman.ttf", "TimesNewRoman.ttf");
            boldFontFile = extractResourceToTempFile("/fonts/TimesNewRoman-Bold.ttf", "TimesNewRoman-Bold.ttf");
            italicFontFile = extractResourceToTempFile("/fonts/TimesNewRoman-Italic.ttf", "TimesNewRoman-Italic.ttf");
            
            logoFile = extractResourceToTempFile("/images/logo.png", "logo.png");
            signatureFile = extractResourceToTempFile("/images/signature.png", "signature.png");
        } catch (Exception e) {
            log.error("⚠️ Failed to extract font/image files. Error: {}", e.getMessage());
        }
    }

    private File extractResourceToTempFile(String resourcePath, String fileName) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir, fileName);

        // Remove the skip check to always extract the latest resource from classpath
        // when the service starts up, preventing stale logos/fonts.

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
            }
            Files.copy(in, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("📋 Extracted classpath resource {} to permanent temp file {}", resourcePath, file.getAbsolutePath());
            return file;
        }
    }

    public byte[] generateCertificatePdfBytes(CertificateIssuedEvent event) throws Exception {
        // 1. Get Classification from event (falling back to calculation if null)
        String classification = event.getClassification();
        double score = event.getFinalScore() != null ? event.getFinalScore() : 0.0;
        if (classification == null || classification.isBlank()) {
            if (score >= 90.0) {
                classification = "Xuất sắc";
            } else if (score >= 80.0) {
                classification = "Giỏi";
            } else if (score >= 65.0) {
                classification = "Khá";
            } else {
                classification = "Trung bình";
            }
        }

        // 2. Prepare Thymeleaf Context
        Context context = new Context();
        context.setVariable("studentName", event.getStudentName());
        context.setVariable("courseName", event.getCourseName());
        context.setVariable("classification", classification);
        context.setVariable("finalScore", score);
        context.setVariable("certificateId", event.getCertificateId());
        context.setVariable("userId", event.getUserId());
        context.setVariable("courseId", event.getCourseId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        String issuedDateStr = formatter.format(event.getIssuedAt() != null ? event.getIssuedAt() : Instant.now());
        context.setVariable("issuedDate", issuedDateStr);

        // Generate verification URL and QR Code
        String verifyUrl = String.format("%s/verify-certificate/%s", frontendUrl, event.getCertificateId());
        
        File tempQrFile = null;
        byte[] qrCodeBytes = QrCodeGenerator.generateQrCodePng(verifyUrl, 100, 100);
        if (qrCodeBytes != null) {
            tempQrFile = File.createTempFile("qrcode-" + event.getCertificateId() + "-", ".png");
            tempQrFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tempQrFile)) {
                fos.write(qrCodeBytes);
            }
            context.setVariable("qrCode", tempQrFile.toURI().toString());
        } else {
            context.setVariable("qrCode", "");
        }
        context.setVariable("verifyUrl", verifyUrl);

        // Inject logo and signature paths
        if (logoFile != null && logoFile.exists()) {
            context.setVariable("logoPath", logoFile.toURI().toString());
        } else {
            context.setVariable("logoPath", "");
        }

        if (signatureFile != null && signatureFile.exists()) {
            context.setVariable("signaturePath", signatureFile.toURI().toString());
        } else {
            context.setVariable("signaturePath", "");
        }

        // 3. Render HTML
        String htmlContent = templateEngine.process("certificate", context);

        // 4. Generate PDF using OpenHTMLtoPDF
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Register custom fonts to support Vietnamese diacritics
            if (regularFontFile != null && regularFontFile.exists()) {
                builder.useFont(regularFontFile, "Times New Roman", 400, FontStyle.NORMAL, true);
            }
            if (boldFontFile != null && boldFontFile.exists()) {
                builder.useFont(boldFontFile, "Times New Roman", 700, FontStyle.NORMAL, true);
            }
            if (italicFontFile != null && italicFontFile.exists()) {
                builder.useFont(italicFontFile, "Times New Roman", 400, FontStyle.ITALIC, true);
            }

            builder.withHtmlContent(htmlContent, "/");
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } finally {
            // Clean up QR code file
            try {
                if (tempQrFile != null && tempQrFile.exists()) {
                    Files.delete(tempQrFile.toPath());
                }
            } catch (Exception e) {
                log.warn("Failed to delete temp QR file: {}", e.getMessage());
            }
        }
    }

    public String generateAndUploadCertificate(CertificateIssuedEvent event) throws Exception {
        log.info("Starting PDF generation for certId={}, student={}, course={}", 
                event.getCertificateId(), event.getStudentName(), event.getCourseName());

        byte[] pdfBytes = generateCertificatePdfBytes(event);

        File tempPdfFile = File.createTempFile("certificate-" + event.getCertificateId() + "-", ".pdf");
        tempPdfFile.deleteOnExit();

        try {
            try (FileOutputStream fos = new FileOutputStream(tempPdfFile)) {
                fos.write(pdfBytes);
            }

            log.info("PDF generated locally at: {}", tempPdfFile.getAbsolutePath());

            // Upload to MinIO with Retry Mechanism (max 3 attempts)
            String remoteKey = "certificates/" + event.getCertificateId() + ".pdf";
            int maxAttempts = 3;
            int attempt = 0;
            long backoffMs = 1000;

            while (attempt < maxAttempts) {
                try {
                    attempt++;
                    log.info("Uploading PDF to MinIO, attempt {}/{}", attempt, maxAttempts);
                    storageStrategy.uploadFile(tempPdfFile.getAbsolutePath(), remoteKey);
                    break;
                } catch (Exception e) {
                    log.warn("MinIO upload attempt {} failed: {}", attempt, e.getMessage());
                    if (attempt < maxAttempts) {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        throw e;
                    }
                }
            }

            // Construct and return MinIO URL
            String fileUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, remoteKey);
            log.info("Certificate PDF successfully uploaded to MinIO. URL: {}", fileUrl);
            return fileUrl;

        } finally {
            // Clean up PDF file
            try {
                if (tempPdfFile.exists()) {
                    Files.delete(tempPdfFile.toPath());
                    log.info("Cleaned up local temp PDF file: {}", tempPdfFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Failed to delete temp PDF file {}: {}", tempPdfFile.getAbsolutePath(), e.getMessage());
            }
        }
    }
}

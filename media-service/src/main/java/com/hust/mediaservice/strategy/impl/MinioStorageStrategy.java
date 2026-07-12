package com.hust.mediaservice.strategy.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.mediaservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.io.IOException;
import java.util.UUID;

@Component(AppConstants.Upload_Strategies.S3)
@RequiredArgsConstructor
@Slf4j
public class MinioStorageStrategy implements StorageStrategy {

    private final AmazonS3 s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.minio.endpoint}")
    private String endpoint;


    @Override
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        ensureBucketExists();

        String prefix = (folderName == null || folderName.isBlank()) ? "" : folderName + "/";
        String fileName = prefix + UUID.randomUUID() + "_" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // Cấu hình để trình duyệt xem trực tiếp (Inline Preview) thay vì tải về
        if (file.getContentType() != null && file.getContentType().equals("application/pdf")) {
            metadata.setContentDisposition("inline; filename=\"" + file.getOriginalFilename() + "\"");
        }

        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return String.format("%s/%s/%s", endpoint, bucketName, fileName);
    }

    private void ensureBucketExists() {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);

            // Thiết lập Policy cho phép đọc công khai (Anonymous Read)
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Sid\": \"PublicRead\",\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": \"*\",\n" +
                    "            \"Action\": [\"s3:GetObject\"],\n" +
                    "            \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            s3Client.setBucketPolicy(bucketName, policy);
            log.info("Created bucket {} with public read policy", bucketName);
        }
    }



    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        String key = fileUrl;
        String bucketPart = bucketName + "/";
        int index = fileUrl.indexOf(bucketPart);
        if (index != -1) {
            key = fileUrl.substring(index + bucketPart.length());
        }
        s3Client.deleteObject(bucketName, key);
    }

    @Override
    public byte[] getFile(String path) throws IOException {
        try (com.amazonaws.services.s3.model.S3Object s3Object = s3Client.getObject(bucketName, path)) {
            return s3Object.getObjectContent().readAllBytes();
        } catch (Exception e) {
            throw new IOException("Failed to fetch file from S3: " + path, e);
        }
    }



    @Override
    public String generatePresignedUploadUrl(String objectKey, int expirationMinutes) {
        ensureBucketExists();

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutes;
        expiration.setTime(expTimeMillis);

        log.info("Generating Presigned PUT URL for object: {} valid for {} minutes", objectKey, expirationMinutes);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}

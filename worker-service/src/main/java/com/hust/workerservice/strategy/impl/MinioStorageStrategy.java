package com.hust.workerservice.strategy.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.workerservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component(AppConstants.Upload_Strategies.S3)
@RequiredArgsConstructor
@Slf4j
public class MinioStorageStrategy implements StorageStrategy {

    private final AmazonS3 s3Client;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.minio.endpoint}")
    private String endpoint;



    private void ensureBucketExists() {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);

            // Public anonymous read policy
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
    public void uploadDirectory(String localPath, String remotePath) throws IOException {
        ensureBucketExists();

        Path sourcePath = Paths.get(localPath);
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String key = remotePath + "/" + sourcePath.relativize(file).toString().replace("\\", "/");
                s3Client.putObject(new PutObjectRequest(bucketName, key, file.toFile())
                        .withCannedAcl(CannedAccessControlList.PublicRead));
                log.info("Uploaded {} to S3", key);
            });
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key;
        String prefix = endpoint + "/" + bucketName + "/";
        if (fileUrl.startsWith(prefix)) {
            key = fileUrl.substring(prefix.length());
        } else {
            key = fileUrl;
        }
        s3Client.deleteObject(bucketName, key);
    }



    @Override
    public void downloadFileToLocal(String path, java.io.File destinationFile) throws IOException {
        log.info("Streaming direct file download from MinIO to local path: {}", destinationFile.getAbsolutePath());
        String key;
        String prefix = endpoint + "/" + bucketName + "/";
        if (path != null && path.startsWith(prefix)) {
            key = path.substring(prefix.length());
        } else {
            key = path;
        }
        try (S3Object s3Object = s3Client.getObject(bucketName, key);
             java.io.InputStream in = s3Object.getObjectContent()) {

            java.nio.file.Files.copy(in, destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Completed direct download of {} from MinIO.", key);
        } catch (Exception e) {
            throw new IOException("Streaming download failed for S3 path: " + key, e);
        }
    }

    @Override
    public void uploadFile(String localFilePath, String remoteKey) throws IOException {
        ensureBucketExists();
        java.io.File file = new java.io.File(localFilePath);
        try {
            s3Client.putObject(new PutObjectRequest(bucketName, remoteKey, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
            log.info("Uploaded single file {} to S3 key {}", localFilePath, remoteKey);
        } catch (Exception e) {
            throw new IOException("Failed to upload file " + localFilePath + " to MinIO key " + remoteKey, e);
        }
    }
}


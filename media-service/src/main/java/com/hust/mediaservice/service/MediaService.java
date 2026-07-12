package com.hust.mediaservice.service;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.mediaservice.dto.response.PresignedUrlResponse;
import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.repository.MediaRepository;
import com.hust.mediaservice.strategy.StorageStrategy;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.entity.enums.StorageProvider;
import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.mediaservice.client.LearningClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    private final MediaRepository mediaRepository;
    private final StorageStrategy storageStrategy;
    private final ApplicationEventPublisher eventPublisher;
    private final LearningClient learningClient;


    public Media upload(MultipartFile file, MediaType type) throws IOException {
        if (type == MediaType.VIDEO) {
            throw new IllegalArgumentException("Phát hiện nỗ lực tải video trực tiếp qua Gateway! Vui lòng sử dụng luồng tối ưu Presigned URL để tránh quá tải RAM.");
        }
        

        Media media = Media.builder()
                .fileName(file.getOriginalFilename())
                .fileType(type)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .provider(StorageProvider.S3)
                .ownerId(getUserId())
                .status(type == MediaType.IMAGE ? Media.MediaStatus.READY : Media.MediaStatus.PENDING)
                .build();
        
        media = mediaRepository.save(media);

        try {
            // 2. Direct lightweight file upload (Categorized by folder)
            String folderName = type.name().toLowerCase() + "s"; // "documents", "pdfs", "images"
            String url = storageStrategy.uploadFile(file, folderName);
            media.setUrl(url);
            return mediaRepository.save(media);
            
        } catch (Exception e) {
            log.error("Failed to process media: {}", media.getId(), e);
            media.setStatus(Media.MediaStatus.FAILED);
            mediaRepository.save(media);
            throw new IOException("Media processing failed", e);
        }
    }

    public Media getById(String id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", id));
    }

    public void deleteMedia(String id) {
        Media media = getById(id);
        
        // 1. Xóa file chính trên MinIO
        if (media.getUrl() != null) {
            try {
                storageStrategy.deleteFile(media.getUrl());
            } catch (Exception e) {
                log.warn("Failed to delete main file from MinIO: {}", e.getMessage());
            }
        }
        
        // 2. Nếu là video, xóa raw file
        if (media.getRawFileKey() != null) {
            try {
                storageStrategy.deleteFile(media.getRawFileKey());
            } catch (Exception e) {
                log.debug("Failed to delete raw video key: {}", e.getMessage());
            }
        }

        // 3. Xóa record trong MongoDB
        mediaRepository.delete(media);
        log.info("🗑️ Media {} deleted successfully.", id);
    }

    public Optional<Media> findByHlsFolderName(String hlsFolderName) {
        return mediaRepository.findByHlsFolderName(hlsFolderName);
    }

    public byte[] getVideoKey(String folderName) throws IOException {
        String keyPath = "hls/" + folderName + "/video.key"; 

        return storageStrategy.getFile(keyPath);
    }

    public PresignedUrlResponse generatePresignedUrlForVideo(String fileName, String contentType) {
        String rawFileKey = "raw-videos/" + UUID.randomUUID()+ "_" + fileName;
        
        log.info("Creating PENDING media asset. Requesting Presigned PUT URL for: {}", rawFileKey);
        String uploadUrl = storageStrategy.generatePresignedUploadUrl(rawFileKey, 15);
        
        Media media = Media.builder()
                .fileName(fileName)
                .fileType(MediaType.VIDEO)
                .contentType(contentType)
                .provider(StorageProvider.S3)
                .ownerId(getUserId())
                .status(Media.MediaStatus.PENDING)
                .rawFileKey(rawFileKey)
                .build();
        
        media = mediaRepository.save(media);
        
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .mediaId(media.getId())
                .objectKey(rawFileKey)
                .build();
    }

    public void linkToLessonAndTriggerProcessing(String mediaId, String lessonId, String courseId) {
        Media media = getById(mediaId);
        
        // Enforce ownership: Only the owner who uploaded the file can link it to a lesson
        String currentUserId = getUserId();
        if (media.getOwnerId() != null && !media.getOwnerId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải chủ sở hữu của tệp tin media này.");
        }
        
        media.setReferenceId(lessonId);
        mediaRepository.save(media);
        log.info("Linked media {} to lesson {}", mediaId, lessonId);

        if (media.getFileType() == MediaType.VIDEO && media.getStatus() == Media.MediaStatus.PENDING) {
            MediaProcessingRequestEvent event = MediaProcessingRequestEvent.builder()
                    .mediaId(mediaId)
                    .fileUrl(media.getRawFileKey())
                    .courseId(courseId)
                    .lessonId(lessonId)
                    .mediaType(MediaType.VIDEO.name())
                    .build();
            eventPublisher.publishEvent(new com.hust.mediaservice.event.MediaProcessingRequestSpringEvent(this, event));
            log.info("📢 Published internal MediaProcessingRequestSpringEvent for VIDEO media {}", mediaId);
            
        } else if (media.getFileType() == MediaType.DOCUMENT) {
            MediaProcessingRequestEvent event = MediaProcessingRequestEvent.builder()
                    .mediaId(mediaId)
                    .fileUrl(media.getUrl())
                    .courseId(courseId)
                    .lessonId(lessonId)
                    .mediaType(MediaType.DOCUMENT.name())
                    .build();
            eventPublisher.publishEvent(new com.hust.mediaservice.event.MediaProcessingRequestSpringEvent(this, event));
            log.info("📢 Published internal MediaProcessingRequestSpringEvent for PDF media {}", mediaId);
        }
    }

    private String getUserId() {
        return SecurityUtils.getCurrentUserIdOrThrow();
    }

    @CustomCache(key = "'media:access:' + #userId + ':' + #lessonId", ttl = 5)
    public boolean checkLessonAccess(String userId, String lessonId) {
        log.debug("🔑 [CustomCache MISS] Checking access for userId {} and lessonId {} via learning-service Feign", userId, lessonId);
        ApiResponse<Boolean> accessResponse = learningClient.checkLessonAccess(userId, lessonId);
        return accessResponse.isSuccess() && Boolean.TRUE.equals(accessResponse.getPayload());
    }
}

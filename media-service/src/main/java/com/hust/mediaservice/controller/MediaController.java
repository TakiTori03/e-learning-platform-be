package com.hust.mediaservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.mediaservice.dto.response.MediaResponse;
import com.hust.mediaservice.dto.response.PresignedUrlResponse;

import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.entity.enums.MediaType;
import com.hust.mediaservice.mapper.MediaMapper;
import com.hust.mediaservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;
    private final MediaMapper mediaMapper;

    // ======================== QUERY ========================

    /**
     * Tra cứu trạng thái & thông tin chi tiết của một media asset.
     * Frontend dùng polling endpoint này để theo dõi quá trình transcode video
     * (PENDING → PROCESSING → READY / FAILED).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaResponse>> getMediaById(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.getById(id)))
                        .build()
        );
    }

    // ======================== UPLOAD ========================

    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.IMAGE)))
                        .build()
        );
    }

    @PostMapping("/upload/document")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                ApiResponse.<MediaResponse>builder()
                        .success(true)
                        .payload(mediaMapper.entityToResponse(mediaService.upload(file, MediaType.DOCUMENT)))
                        .build()
        );
    }

    /**
     * High-Scale Endpoint: Request a Presigned temporary S3/MinIO URL to upload video
     * directly from browser, completely bypassing the API Gateway memory load.
     */
    @PostMapping("/upload/request-presigned-url")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> requestUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType) {
        
        return ResponseEntity.ok(
                ApiResponse.<PresignedUrlResponse>builder()
                        .success(true)
                        .payload(mediaService.generatePresignedUrlForVideo(fileName, contentType))
                        .build()
        );
    }

    /**
     * Public API used by Frontend to link a media asset to a lesson 
     * after the lesson is created (Late Binding).
     */
    @PutMapping("/{mediaId}/link")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> linkMediaToLesson(
            @PathVariable String mediaId, 
            @RequestParam String lessonId,
            @RequestParam String courseId) {
        
        mediaService.linkToLessonAndTriggerProcessing(mediaId, lessonId, courseId);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .payload("Media đã được liên kết với bài học thành công.")
                        .build()
        );
    }

    // triggerAsyncProcessing has been removed. Processing is now automatically 
    // triggered by course-service when a lesson is created via Kafka.

    // ======================== DELETE ========================

    /**
     * Xóa media asset: xóa file trên MinIO + xóa record trong MongoDB.
     * Chỉ INSTRUCTOR (chủ sở hữu) hoặc ADMIN mới được phép xóa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable String id)  {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .payload("Media đã được xóa thành công.")
                        .build()
        );
    }

    // ======================== DRM / KEY ========================

    @GetMapping("/keys/{folderName}")
    public ResponseEntity<byte[]> getVideoKey(@PathVariable String folderName) throws IOException {
        Media media = mediaService.findByHlsFolderName(folderName)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "hlsFolderName", folderName));

        String lessonId = media.getReferenceId();
        if (lessonId == null) {
            throw new ResourceNotFoundException("Media không được liên kết với bài giảng nào.");
        }

        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        
        boolean isAdmin = SecurityUtils.isAdmin();
        
        boolean isOwner = userId.equals(media.getOwnerId());
        
        boolean isAllowed = isAdmin || isOwner || mediaService.checkLessonAccess(userId, lessonId);
        
        if (!isAllowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] keyBytes = mediaService.getVideoKey(folderName);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(keyBytes);
    }
}

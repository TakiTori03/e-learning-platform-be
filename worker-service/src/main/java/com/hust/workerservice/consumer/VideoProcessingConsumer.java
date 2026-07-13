package com.hust.workerservice.consumer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.hust.commonlibrary.annotation.TrackPerformance;
import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.commonlibrary.event.MediaProcessingRequestEvent;
import com.hust.commonlibrary.event.SttRequestEvent;
import com.hust.workerservice.service.MediaProcessingStatusService;
import com.hust.workerservice.service.VideoProcessingService;
import com.hust.workerservice.strategy.StorageStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer xử lý sự kiện Video từ Kafka.
 *
 * Pipeline song song (Tối ưu 2):
 * 1. Tải video gốc từ MinIO về máy cục bộ.
 * 2. Tách luồng âm thanh WAV siêu nhẹ 16kHz NGAY LẬP TỨC.
 * 3. Upload WAV lên MinIO + Phát sự kiện STT Request qua Kafka (bất đồng bộ cho GPU xử lý).
 * 4. ĐỒNG THỜI chạy HLS Transcoding (CPU) song song với GPU STT.
 * 5. Khi HLS xong -> Đồng bộ thư mục lên MinIO -> Đánh dấu Redis.
 * 6. Bên nào xong SAU CÙNG (HLS hoặc STT) sẽ phát LessonMediaReadyEvent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingConsumer {

    private final VideoProcessingService videoProcessingService;
    private final StorageStrategy storageStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MediaProcessingStatusService statusService;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    @Value("${app.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${app.minio.bucket-name:}")
    private String bucketName;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {
                org.springframework.web.client.RestClientException.class,
                java.io.IOException.class,
                java.lang.RuntimeException.class
            }
    )
    @KafkaListener(topics = KafkaTopics.MEDIA_PROCESSING, groupId = "video-worker-group", concurrency = "1")
    @TrackPerformance(threshold = 600000, description = "Video HLS Transcoding & Kafka STT Pipeline (Parallel)")
    public void consume(MediaProcessingRequestEvent event) {
        if (!"VIDEO".equalsIgnoreCase(event.getMediaType())) {
            return;
        }

        log.info("🎬 Nhận sự kiện xử lý Video từ Kafka: {}", event);

        File tempFile = null;
        String hlsFolderName = UUID.randomUUID().toString();
        Path localHlsPath = Paths.get(baseStoragePath, "hls", hlsFolderName);

        try {
            // 1. Khởi tạo trạng thái trên Redis (HLS=PENDING, STT=PENDING)
            statusService.initVideoProcessingStatus(
                    event.getMediaId(), event.getCourseId(), event.getLessonId(), hlsFolderName);

            // 2. Tạo tệp tạm và tải video gốc từ MinIO về máy cục bộ
            tempFile = File.createTempFile("worker-transcode-raw-", ".mp4");
            storageStrategy.downloadFileToLocal(event.getFileUrl(), tempFile);

            // 3. Tách luồng âm thanh WAV 16kHz NGAY LẬP TỨC (trước khi HLS)
            String relativeAudioPath = videoProcessingService.extractAudio(tempFile, hlsFolderName);
            File audioFile = Paths.get(baseStoragePath, relativeAudioPath).toFile();

            // 4. Upload WAV lên MinIO + Phát sự kiện STT Request qua Kafka
            String audioKey = "hls/" + hlsFolderName + "/audio.wav";
            storageStrategy.uploadFile(audioFile.getAbsolutePath(), audioKey);
            log.info("📤 Đã upload file WAV lên MinIO key: {}", audioKey);

            SttRequestEvent sttRequest = SttRequestEvent.builder()
                    .mediaId(event.getMediaId())
                    .courseId(event.getCourseId())
                    .lessonId(event.getLessonId())
                    .audioKey(audioKey)
                    .language("auto")
                    .build();
            kafkaTemplate.send(KafkaTopics.STT_REQUESTS, event.getLessonId(), sttRequest);
            log.info("📨 Đã phát SttRequestEvent qua Kafka cho GPU Service bóc băng bất đồng bộ.");

            // 5. ĐỒNG THỜI chạy HLS Transcoding trên CPU (song song với GPU STT)
            videoProcessingService.processToHls(tempFile, hlsFolderName);
            log.info("✅ HLS Transcoding hoàn tất cho folder: {}", hlsFolderName);

            // 6. Đồng bộ toàn bộ thư mục HLS lên MinIO
            String remoteHlsPath = "hls/" + hlsFolderName;
            storageStrategy.uploadDirectory(localHlsPath.toString(), remoteHlsPath);

            // 7. Tạo liên kết công khai
            String hlsUrl = String.format("%s/%s/%s/playlist.m3u8", minioEndpoint, bucketName, remoteHlsPath);
            long fileSize = tempFile.length();
            Double duration = videoProcessingService.getVideoDuration(tempFile);

            // 8. Đánh dấu HLS hoàn tất trên Redis và kiểm tra STT đã xong chưa
            boolean allDone = statusService.markHlsComplete(
                    event.getMediaId(), hlsUrl, fileSize, duration != null ? duration : 0.0);

            if (allDone) {
                // Nếu STT cũng đã xong -> Phát sự kiện hoàn tất
                emitFinalReadyEvent(event.getMediaId(), event.getLessonId());
            } else {
                log.info("⏳ HLS đã xong. STT vẫn đang chạy trên GPU. SttResultConsumer sẽ phát event khi STT hoàn thành.");
            }

            // 9. Dọn dẹp video thô trên MinIO
            try {
                storageStrategy.deleteFile(event.getFileUrl());
                log.info("🗑️ Đã xóa video gốc thô trên MinIO: {}", event.getFileUrl());
            } catch (Exception ex) {
                log.warn("⚠️ Không thể xóa video thô {} trên MinIO: {}", event.getFileUrl(), ex.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Lỗi nghiêm trọng khi xử lý video {}: {}", event.getMediaId(), e.getMessage(), e);
            // Đánh dấu HLS thất bại trên Redis
            try {
                boolean allDone = statusService.markHlsFailed(event.getMediaId());
                if (allDone) {
                    emitFinalReadyEvent(event.getMediaId(), event.getLessonId());
                }
            } catch (Exception redisEx) {
                log.error("❌ Lỗi cập nhật Redis khi HLS thất bại: {}", redisEx.getMessage());
            }
            throw new RuntimeException("Video HLS transcoding failed, triggering retry topic", e);
        } finally {
            // Dọn dẹp tài nguyên tệp tạm cục bộ
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            if (Files.exists(localHlsPath)) {
                FileSystemUtils.deleteRecursively(localHlsPath.toFile());
                log.info("🧹 Đã dọn dẹp thư mục HLS tạm cục bộ: {}", localHlsPath);
            }
        }
    }

    /**
     * Phát sự kiện LessonMediaReadyEvent dựa trên trạng thái Redis.
     * Được gọi khi cả HLS và STT đều đã hoàn thành (SUCCESS hoặc FAILED).
     */
    public void emitFinalReadyEvent(String mediaId, String lessonId) {
        try {
            if (statusService.hasAnyFailure(mediaId)) {
                // Có ít nhất 1 bước thất bại -> gửi event báo lỗi
                LessonMediaReadyEvent failedEvent = LessonMediaReadyEvent.builder()
                        .lessonId(lessonId)
                        .mediaId(mediaId)
                        .url(null)
                        .fileSize(-1L)
                        .build();
                kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, lessonId, failedEvent);
                log.warn("📢 Đã gửi LessonMediaReadyEvent báo lỗi (FAILED) cho lesson {}", lessonId);
            } else {
                // Cả 2 bước đều thành công -> gửi event thành công
                String hlsUrl = statusService.getField(mediaId, MediaProcessingStatusService.FIELD_HLS_URL);
                String transcriptUrl = statusService.getField(mediaId, MediaProcessingStatusService.FIELD_TRANSCRIPT_URL);
                String fileSizeStr = statusService.getField(mediaId, MediaProcessingStatusService.FIELD_FILE_SIZE);
                String durationStr = statusService.getField(mediaId, MediaProcessingStatusService.FIELD_DURATION);
                String hlsFolder = statusService.getField(mediaId, MediaProcessingStatusService.FIELD_HLS_FOLDER);

                long fileSize = fileSizeStr != null ? Long.parseLong(fileSizeStr) : 0L;
                double duration = durationStr != null ? Double.parseDouble(durationStr) : 0.0;

                LessonMediaReadyEvent readyEvent = LessonMediaReadyEvent.builder()
                        .lessonId(lessonId)
                        .mediaId(mediaId)
                        .hlsFolderName(hlsFolder)
                        .transcriptUrl(transcriptUrl)
                        .url(hlsUrl)
                        .fileSize(fileSize)
                        .duration(duration)
                        .build();
                kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, lessonId, readyEvent);
                log.info("✅ Đã phát LessonMediaReadyEvent thành công cho lesson {}", lessonId);
            }
            // Dọn dẹp Redis
            statusService.cleanup(mediaId);
        } catch (Exception e) {
            log.error("❌ Lỗi khi phát LessonMediaReadyEvent: {}", e.getMessage(), e);
        }
    }

    @DltHandler
    public void handleDlt(MediaProcessingRequestEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        if (!"VIDEO".equalsIgnoreCase(event.getMediaType())) {
            return;
        }
        log.error("❌ [DLQ] Xử lý Video/STT cho lesson {} thất bại hoàn toàn sau tất cả các lần thử lại tại topic: {}", event.getLessonId(), topic);
        try {
            LessonMediaReadyEvent failedEvent = LessonMediaReadyEvent.builder()
                    .lessonId(event.getLessonId())
                    .mediaId(event.getMediaId())
                    .url(null)
                    .fileSize(-1L)
                    .build();
            kafkaTemplate.send(KafkaTopics.LESSON_MEDIA_READY, event.getLessonId(), failedEvent);
            log.warn("📢 Đã gửi LessonMediaReadyEvent báo lỗi (DLQ FAILED) cho lesson {}", event.getLessonId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý DLQ Handler cho Video: {}", e.getMessage(), e);
        }
    }
}

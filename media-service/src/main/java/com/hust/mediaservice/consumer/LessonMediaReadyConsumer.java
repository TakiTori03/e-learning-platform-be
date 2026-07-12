package com.hust.mediaservice.consumer;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.event.LessonMediaReadyEvent;
import com.hust.mediaservice.entity.Media;
import com.hust.mediaservice.repository.MediaRepository;
import com.hust.mediaservice.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LessonMediaReadyConsumer {

    private final MediaRepository mediaRepository;
    private final StorageStrategy storageStrategy;

    @KafkaListener(topics = KafkaTopics.LESSON_MEDIA_READY, groupId = "media-group")
    public void consume(LessonMediaReadyEvent event) {
        log.info("📥 Received LessonMediaReadyEvent from Kafka: {}", event);

        if (event.getMediaId() == null) {
            log.warn("Media ID is null, skipping update");
            return;
        }

        try {
            mediaRepository.findById(event.getMediaId()).ifPresentOrElse(media -> {
                if (event.getFileSize() != null && event.getFileSize() < 0) {
                    media.setStatus(Media.MediaStatus.FAILED);
                    log.warn("⚠️ Media processing failed for ID: {}", event.getMediaId());
                } else {
                    media.setStatus(Media.MediaStatus.READY);
                }

                if (event.getUrl() != null) {
                    media.setUrl(event.getUrl());
                }

                if (event.getFileSize() != null && event.getFileSize() >= 0) {
                    media.setFileSize(event.getFileSize());
                }
                if (event.getDuration() != null) {
                    media.setDuration(event.getDuration());
                }
                media.setHlsFolderName(event.getHlsFolderName());
                media.setTranscriptUrl(event.getTranscriptUrl());

                mediaRepository.save(media);
                log.info("✅ Updated Media {} status to READY (Size: {}, Duration: {})", event.getMediaId(), event.getFileSize(), event.getDuration());

                // Optional: purge raw temporary file if needed, but worker already handles cleanups.
                if (media.getRawFileKey() != null) {
                    try {
                        storageStrategy.deleteFile(media.getRawFileKey());
                        log.info("🧹 Purged raw video key from MinIO: {}", media.getRawFileKey());
                    } catch (Exception ex) {
                        log.warn("Failed to delete raw video key: {}", ex.getMessage());
                    }
                }
            }, () -> log.error("❌ Media not found in database for ID: {}", event.getMediaId()));

        } catch (Exception e) {
            log.error("❌ Error updating Media status for ID {}: {}", event.getMediaId(), e.getMessage(), e);
        }
    }
}

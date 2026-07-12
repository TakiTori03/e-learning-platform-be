package com.hust.workerservice.consumer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.KafkaTopics;
import com.hust.commonlibrary.entity.ContentType;
import com.hust.commonlibrary.event.RawTextIngestedEvent;
import com.hust.commonlibrary.event.SttResultEvent;
import com.hust.workerservice.service.MediaProcessingStatusService;
import com.hust.workerservice.strategy.StorageStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer lắng nghe kết quả bóc băng STT từ Python stt-service qua Kafka.
 *
 * Luồng xử lý:
 * 1. Nhận nội dung WebVTT từ topic stt-results.
 * 2. Lưu file subtitles.vtt cục bộ và đẩy lên MinIO.
 * 3. Phân mảnh phụ đề thành các Semantic Chunks và phát RawTextIngestedEvent.
 * 4. Đánh dấu STT hoàn tất trên Redis.
 * 5. Nếu HLS cũng đã xong -> phát LessonMediaReadyEvent báo hoàn tất.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SttResultConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StorageStrategy storageStrategy;
    private final MediaProcessingStatusService statusService;
    private final VideoProcessingConsumer videoProcessingConsumer;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    @Value("${app.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${app.minio.bucket-name:}")
    private String bucketName;

    @KafkaListener(
            topics = KafkaTopics.STT_RESULTS,
            groupId = "stt-result-worker-group",
            concurrency = "1",
            properties = {"spring.json.value.default.type=com.hust.commonlibrary.event.SttResultEvent"}
    )
    public void consume(SttResultEvent event) {
        log.info("🎙️ Nhận kết quả STT từ Python cho media: {} (success={})", event.getMediaId(), event.isSuccess());

        if (!event.isSuccess()) {
            log.error("❌ STT thất bại cho media {}: {}", event.getMediaId(), event.getErrorMessage());
            boolean allDone = statusService.markSttFailed(event.getMediaId());
            if (allDone) {
                videoProcessingConsumer.emitFinalReadyEvent(event.getMediaId(), event.getLessonId());
            }
            return;
        }

        String vttContent = event.getVttContent();
        String hlsFolderName = event.getHlsFolderName();

        try {
            // 1. Lưu tệp WebVTT cục bộ
            Path vttPath = Paths.get(baseStoragePath, "hls", hlsFolderName, "subtitles.vtt");
            Files.createDirectories(vttPath.getParent());
            Files.write(vttPath, vttContent.getBytes());
            log.info("💾 Đã lưu tệp WebVTT phụ đề: {}", vttPath.toAbsolutePath());

            // 2. Upload subtitles.vtt lên MinIO
            String remoteVttKey = "hls/" + hlsFolderName + "/subtitles.vtt";
            storageStrategy.uploadFile(vttPath.toAbsolutePath().toString(), remoteVttKey);
            String transcriptUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, remoteVttKey);

            // 3. Phân mảnh phụ đề WebVTT thành các Semantic Chunks
            parseAndEmitVttChunks(vttContent, event);

            // 4. Đánh dấu STT hoàn tất trên Redis
            boolean allDone = statusService.markSttComplete(event.getMediaId(), transcriptUrl);

            if (allDone) {
                // HLS cũng đã xong -> phát sự kiện hoàn tất
                videoProcessingConsumer.emitFinalReadyEvent(event.getMediaId(), event.getLessonId());
            } else {
                log.info("⏳ STT đã xong. HLS vẫn đang transcode. VideoProcessingConsumer sẽ phát event khi HLS hoàn thành.");
            }

            // 5. Dọn dẹp file VTT tạm cục bộ
            Files.deleteIfExists(vttPath);

        } catch (Exception e) {
            log.error("❌ Lỗi xử lý kết quả STT cho media {}: {}", event.getMediaId(), e.getMessage(), e);
            boolean allDone = statusService.markSttFailed(event.getMediaId());
            if (allDone) {
                videoProcessingConsumer.emitFinalReadyEvent(event.getMediaId(), event.getLessonId());
            }
        }
    }

    /**
     * Phân mảnh thông minh phụ đề WebVTT thành các chunk ~120 từ kèm mốc thời gian.
     */
    private void parseAndEmitVttChunks(String vttContent, SttResultEvent event) {
        log.info("🎞️ Bắt đầu phân mảnh thông minh phụ đề WebVTT...");
        if (vttContent == null || vttContent.trim().isEmpty()) {
            return;
        }

        String[] blocks = vttContent.split("\\r?\\n\\r?\\n");
        StringBuilder currentChunk = new StringBuilder();
        String currentStartTimestamp = null;
        int wordCount = 0;
        int chunkIdx = 0;

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty() || block.equals("WEBVTT")) {
                continue;
            }

            String[] lines = block.split("\\r?\\n");
            if (lines.length < 2) {
                continue;
            }

            String timeLine = lines[0];
            if (!timeLine.contains("-->")) {
                continue;
            }

            StringBuilder textLineBuilder = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                textLineBuilder.append(lines[i]).append(" ");
            }
            String cueText = textLineBuilder.toString().trim();
            if (cueText.isEmpty()) {
                continue;
            }

            String startRaw = timeLine.split("-->")[0].trim();
            String formattedTime = formatTimestamp(startRaw);

            if (currentStartTimestamp == null) {
                currentStartTimestamp = formattedTime;
            }

            currentChunk.append(cueText).append(" ");
            wordCount += cueText.split("\\s+").length;

            // Gom khoảng 120 từ (~1 đến 1.5 phút thuyết giảng)
            if (wordCount >= 120) {
                emitRawTextEvent(event, currentChunk.toString().trim(), currentStartTimestamp, chunkIdx++);
                currentChunk = new StringBuilder();
                currentStartTimestamp = null;
                wordCount = 0;
            }
        }

        // Emit nốt chunk cuối cùng còn sót lại
        if (!currentChunk.isEmpty() && currentStartTimestamp != null) {
            emitRawTextEvent(event, currentChunk.toString().trim(), currentStartTimestamp, chunkIdx++);
        }
    }

    private String formatTimestamp(String rawTime) {
        // "00:02:15.500" -> "02:15"
        String[] parts = rawTime.split(":");
        if (parts.length >= 3) {
            String minutes = parts[1];
            String seconds = parts[2];
            if (seconds.contains(".")) {
                seconds = seconds.substring(0, seconds.indexOf("."));
            }
            return minutes + ":" + seconds;
        }
        return "00:00";
    }

    private void emitRawTextEvent(SttResultEvent event, String text, String timestamp, int chunkIndex) {
        RawTextIngestedEvent ingestionEvent = RawTextIngestedEvent.builder()
                .courseId(event.getCourseId())
                .lessonId(event.getLessonId())
                .mediaId(event.getMediaId())
                .content(text)
                .contentType(ContentType.VIDEO)
                .sourceCitation(timestamp)
                .chunkIndex(chunkIndex)
                .build();

        kafkaTemplate.send(KafkaTopics.RAW_TEXT_INGESTED, event.getLessonId(), ingestionEvent);
        log.info("✅ Đã phát RawTextIngestedEvent (Video) tại mốc thời gian: [{}], Chỉ mục chunk: [{}]", timestamp, chunkIndex);
    }
}

package com.hust.workerservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Dịch vụ quản lý trạng thái xử lý Media (Video/PDF) trên Redis.
 * Sử dụng Redis Hash để đồng bộ kết quả giữa 2 tiến trình song song:
 * - HLS Transcoding (CPU - Java Worker)
 * - STT/PDF Parsing (GPU - Python Service)
 *
 * Bên nào hoàn thành sau cùng sẽ phát sự kiện LessonMediaReadyEvent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaProcessingStatusService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "media:status:";
    private static final Duration TTL = Duration.ofHours(24);

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // Hash field constants
    public static final String FIELD_HLS_STATUS = "hls_status";
    public static final String FIELD_STT_STATUS = "stt_status";
    public static final String FIELD_HLS_URL = "hls_url";
    public static final String FIELD_TRANSCRIPT_URL = "transcript_url";
    public static final String FIELD_FILE_SIZE = "file_size";
    public static final String FIELD_DURATION = "duration";
    public static final String FIELD_HLS_FOLDER = "hls_folder";
    public static final String FIELD_COURSE_ID = "course_id";
    public static final String FIELD_LESSON_ID = "lesson_id";

    /**
     * Khởi tạo trạng thái ban đầu khi bắt đầu xử lý video.
     * Cả 2 tiến trình (HLS + STT) đều ở trạng thái PENDING.
     */
    public void initVideoProcessingStatus(String mediaId, String courseId, String lessonId, String hlsFolderName) {
        String key = KEY_PREFIX + mediaId;
        redisTemplate.opsForHash().putAll(key, Map.of(
                FIELD_HLS_STATUS, STATUS_PENDING,
                FIELD_STT_STATUS, STATUS_PENDING,
                FIELD_HLS_FOLDER, hlsFolderName,
                FIELD_COURSE_ID, courseId,
                FIELD_LESSON_ID, lessonId
        ));
        redisTemplate.expire(key, TTL);
        log.info("📋 [Redis] Khởi tạo trạng thái xử lý media: {}", key);
    }

    /**
     * Cập nhật trạng thái của một trường cụ thể.
     */
    public void updateField(String mediaId, String field, String value) {
        String key = KEY_PREFIX + mediaId;
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Đọc giá trị của một trường cụ thể.
     */
    public String getField(String mediaId, String field) {
        String key = KEY_PREFIX + mediaId;
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    /**
     * Đọc toàn bộ trạng thái của media.
     */
    public Map<Object, Object> getAllFields(String mediaId) {
        String key = KEY_PREFIX + mediaId;
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Đánh dấu HLS hoàn thành và kiểm tra xem STT đã xong chưa.
     * @return true nếu CẢ HAI tiến trình đều đã hoàn thành (SUCCESS hoặc FAILED).
     */
    public boolean markHlsComplete(String mediaId, String hlsUrl, long fileSize, double duration) {
        updateField(mediaId, FIELD_HLS_STATUS, STATUS_SUCCESS);
        updateField(mediaId, FIELD_HLS_URL, hlsUrl);
        updateField(mediaId, FIELD_FILE_SIZE, String.valueOf(fileSize));
        updateField(mediaId, FIELD_DURATION, String.valueOf(duration));
        log.info("✅ [Redis] HLS Transcoding hoàn tất cho media: {}", mediaId);
        return isAllComplete(mediaId);
    }

    /**
     * Đánh dấu HLS thất bại.
     * @return true nếu cả hai tiến trình đều đã kết thúc (SUCCESS hoặc FAILED).
     */
    public boolean markHlsFailed(String mediaId) {
        updateField(mediaId, FIELD_HLS_STATUS, STATUS_FAILED);
        log.error("❌ [Redis] HLS Transcoding thất bại cho media: {}", mediaId);
        return isAllComplete(mediaId);
    }

    /**
     * Đánh dấu STT hoàn thành và kiểm tra xem HLS đã xong chưa.
     * @return true nếu CẢ HAI tiến trình đều đã hoàn thành.
     */
    public boolean markSttComplete(String mediaId, String transcriptUrl) {
        updateField(mediaId, FIELD_STT_STATUS, STATUS_SUCCESS);
        updateField(mediaId, FIELD_TRANSCRIPT_URL, transcriptUrl);
        log.info("✅ [Redis] STT Transcription hoàn tất cho media: {}", mediaId);
        return isAllComplete(mediaId);
    }

    /**
     * Đánh dấu STT thất bại.
     * @return true nếu cả hai tiến trình đều đã kết thúc.
     */
    public boolean markSttFailed(String mediaId) {
        updateField(mediaId, FIELD_STT_STATUS, STATUS_FAILED);
        log.error("❌ [Redis] STT Transcription thất bại cho media: {}", mediaId);
        return isAllComplete(mediaId);
    }

    /**
     * Kiểm tra xem cả hai tiến trình đã kết thúc chưa (không phải PENDING nữa).
     */
    public boolean isAllComplete(String mediaId) {
        String hlsStatus = getField(mediaId, FIELD_HLS_STATUS);
        String sttStatus = getField(mediaId, FIELD_STT_STATUS);
        boolean done = hlsStatus != null && !STATUS_PENDING.equals(hlsStatus)
                    && sttStatus != null && !STATUS_PENDING.equals(sttStatus);
        if (done) {
            log.info("🏁 [Redis] Cả 2 tiến trình (HLS + STT) đều đã hoàn tất cho media: {}", mediaId);
        }
        return done;
    }

    /**
     * Kiểm tra xem có tiến trình nào thất bại không.
     */
    public boolean hasAnyFailure(String mediaId) {
        String hlsStatus = getField(mediaId, FIELD_HLS_STATUS);
        String sttStatus = getField(mediaId, FIELD_STT_STATUS);
        return STATUS_FAILED.equals(hlsStatus) || STATUS_FAILED.equals(sttStatus);
    }

    /**
     * Xóa trạng thái sau khi phát sự kiện hoàn tất để giải phóng bộ nhớ Redis.
     */
    public void cleanup(String mediaId) {
        String key = KEY_PREFIX + mediaId;
        redisTemplate.delete(key);
        log.info("🧹 [Redis] Đã dọn dẹp trạng thái xử lý media: {}", key);
    }
}

package com.hust.learningservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.learningservice.dto.request.LessonProgressRequest;
import com.hust.learningservice.entity.LessonProgress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.service.RedisService;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Tối ưu hóa: Write-Behind Caching (Batch Update) cho Tiến Độ Video
 * Quét Redis mỗi phút để Bulk Upsert mốc thời gian xem video (lastWatchedTime)
 * xuống MongoDB. Đạt ngưỡng chịu tải vô cực, loại bỏ hoàn toàn lỗi N+1 Query.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LessonProgressSyncScheduler {

    private final RedisService redisService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    // Chạy mỗi 60 giây (1 phút)
    @Scheduled(fixedDelay = 60000)
    public void syncProgressToMongo() {
        Set<String> fullKeys = redisService.keys(RedisPrefixConstants.LOCAL_LESSON_PROGRESS_SYNC + "*");

        if (fullKeys == null || fullKeys.isEmpty()) {
            return;
        }

        log.info("🚀 [Write-Behind] Đang Bulk Upsert {} bản ghi tiến độ (lastWatchedTime) xuống DB.", fullKeys.size());

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LessonProgress.class);
        List<String> keysToDelete = new ArrayList<>();
        int validCount = 0;

        for (String fullKey : fullKeys) {
            try {
                // Tách key ra các thành phần
                int prefixIndex = fullKey.indexOf(RedisPrefixConstants.LOCAL_LESSON_PROGRESS_SYNC);
                if (prefixIndex == -1) continue;

                String internalKey = fullKey.substring(prefixIndex);
                String[] parts = internalKey.split(":");
                if (parts.length < 5) continue;

                // progress:sync:{userId}:{courseId}:{lessonId}
                String userId = parts[2];
                String courseId = parts[3];
                String lessonId = parts[4];

                // Đọc dữ liệu JSON (lastWatchedTime) từ Redis
                Object cachedObj = redisService.get(internalKey);
                if (cachedObj == null) continue;

                LessonProgressRequest request;
                if (cachedObj instanceof String) {
                    request = objectMapper.readValue((String) cachedObj, LessonProgressRequest.class);
                } else {
                    request = objectMapper.convertValue(cachedObj, LessonProgressRequest.class);
                }

                if (request.getLastWatchedTime() != null) {
                    // Tạo điều kiện tìm kiếm
                    Query query = new Query(Criteria.where("userId").is(userId)
                            .and("courseId").is(courseId)
                            .and("lessonId").is(lessonId));

                    // Cập nhật giá trị
                    Update update = new Update()
                            .set("lastWatchedTime", request.getLastWatchedTime())
                            .currentDate("updatedAt") // Update lại thời gian sửa
                            .setOnInsert("isDone", false)
                            .setOnInsert("createdAt", new Date());

                    // Đưa vào hàng chờ Bulk Upsert
                    bulkOps.upsert(query, update);
                    validCount++;
                }

                keysToDelete.add(internalKey);

            } catch (Exception e) {
                log.error("⚠️ Lỗi khi xử lý tiến độ video từ Redis (Key: {}): {}", fullKey, e.getMessage());
            }
        }

        // Thực thi TẤT CẢ các câu lệnh Upsert vào MongoDB trong MỘT NHÁT CHẠY DUY NHẤT!
        if (validCount > 0) {
            bulkOps.execute();
            log.info("✅ [Write-Behind] Đã Bulk Upsert thành công {} mốc thời gian xem xuống MongoDB (Zero N+1 Query).", validCount);
        }

        // Xóa các key khỏi Redis sau khi đã đồng bộ an toàn
        if (!keysToDelete.isEmpty()) {
            for (String key : keysToDelete) {
                redisService.delete(key);
            }
        }
    }
}

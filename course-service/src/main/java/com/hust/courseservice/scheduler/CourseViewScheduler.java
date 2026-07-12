package com.hust.courseservice.scheduler;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.service.RedisService;
import com.hust.courseservice.entity.Course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseViewScheduler {

    private final RedisService redisService;
    private final MongoTemplate mongoTemplate;

    @Value("${spring.application.name:course-service}")
    private String applicationName;

    /**
     * Sync views from Redis to MongoDB.
     * Default runs every 1 minute.
     * Highly optimized using Redis Multi-Get and MongoDB BulkOperations to resolve N+1 I/O bottlenecks.
     */
    @Scheduled(fixedDelayString = "${app.course.view-sync-delay-ms:60000}")
    public void syncViewsToMongoDB() {
        log.debug("Starting scheduled job: Sync course views from Redis to MongoDB");

        // Utilizes RedisService abstraction to fetch keys automatically prefixed
        Set<String> keysSet = redisService.keys("course:views:count:*");

        if (keysSet == null || keysSet.isEmpty()) {
            log.debug("No course view keys found in Redis to sync.");
            return;
        }

        List<String> keysList = new ArrayList<>(keysSet);

        // 🚀 OPTIMIZATION 1: Multi-Get to fetch all view counters in 1 Single Network Roundtrip to Redis!
        List<Object> values = redisService.multiGet(keysList);

        String prefix = AppConstants.Redis_Constants.APP_PREFIX + applicationName + ":course:views:count:";
        List<Map.Entry<String, Long>> updatesToSync = new ArrayList<>();

        for (int i = 0; i < keysList.size(); i++) {
            String key = keysList.get(i);
            Object valObj = values.get(i);
            if (valObj == null) {
                continue;
            }

            long count = 0;
            if (valObj instanceof Number) {
                count = ((Number) valObj).longValue();
            } else {
                try {
                    count = Long.parseLong(valObj.toString());
                } catch (NumberFormatException e) {
                    log.error("Invalid view count in Redis for key {}: {}", key, valObj);
                    continue;
                }
            }

            if (count <= 0) {
                redisService.delete(key);
                continue;
            }

            updatesToSync.add(new AbstractMap.SimpleEntry<>(key, count));
        }

        if (updatesToSync.isEmpty()) {
            return;
        }

        // 🚀 OPTIMIZATION 2: BulkOperations to execute all MongoDB updates in 1 Single Network Roundtrip!
        try {
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Course.class);

            for (Map.Entry<String, Long> updateInfo : updatesToSync) {
                String key = updateInfo.getKey();
                String courseId = key.substring(prefix.length());
                long count = updateInfo.getValue();

                Query query = new Query(Criteria.where("id").is(courseId));
                Update update = new Update().inc("views", count);
                bulkOps.updateOne(query, update);
            }

            // Execute batch update in MongoDB
            bulkOps.execute();

            // 🚀 OPTIMIZATION 3: Safely decrement and clean up views on Redis
            for (Map.Entry<String, Long> updateInfo : updatesToSync) {
                String key = updateInfo.getKey();
                long count = updateInfo.getValue();

                Long remaining = redisService.decrement(key, count);
                if (remaining != null && remaining <= 0) {
                    redisService.delete(key);
                }
            }

            log.info("Successfully synced views for {} courses to MongoDB via Bulk Update.", updatesToSync.size());
        } catch (Exception e) {
            log.error("Error executing bulk update for course views to MongoDB: ", e);
        }
    }
}

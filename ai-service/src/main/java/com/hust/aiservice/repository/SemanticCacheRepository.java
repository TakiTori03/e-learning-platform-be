package com.hust.aiservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SemanticCacheRepository {

    private final JdbcTemplate jdbcTemplate;

    public SemanticCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findSimilarQuery(String embeddingString, String courseId, double threshold) {
        if (courseId == null || courseId.isBlank()) {
            String sql = "SELECT id, query, answer, citations, (embedding <=> cast(? as vector)) as distance " +
                         "FROM semantic_cache " +
                         "WHERE course_id IS NULL " +
                         "AND (embedding <=> cast(? as vector)) <= ? " +
                         "ORDER BY distance ASC LIMIT 1";
            return jdbcTemplate.queryForList(sql, embeddingString, embeddingString, threshold);
        } else {
            String sql = "SELECT id, query, answer, citations, (embedding <=> cast(? as vector)) as distance " +
                         "FROM semantic_cache " +
                         "WHERE course_id = ? " +
                         "AND (embedding <=> cast(? as vector)) <= ? " +
                         "ORDER BY distance ASC LIMIT 1";
            return jdbcTemplate.queryForList(sql, embeddingString, courseId, embeddingString, threshold);
        }
    }

    public void updateHitCount(Long cacheId) {
        jdbcTemplate.update("UPDATE semantic_cache SET hits_count = hits_count + 1, last_accessed_at = NOW() WHERE id = ?", cacheId);
    }

    public void insertCache(String courseId, String query, String answer, String citationsJson, String embeddingString, String embeddingModel) {
        String sql = "INSERT INTO semantic_cache (course_id, query, answer, citations, embedding, embedding_model, hits_count, last_accessed_at) " +
                     "VALUES (?, ?, ?, cast(? as jsonb), cast(? as vector), ?, 1, NOW())";
        jdbcTemplate.update(sql, courseId, query, answer, citationsJson, embeddingString, embeddingModel);
    }

    public void deleteByLastAccessedAtBefore(java.time.Instant cutoff) {
        String sql = "DELETE FROM semantic_cache WHERE last_accessed_at < ?";
        jdbcTemplate.update(sql, java.sql.Timestamp.from(cutoff));
    }

    public void deleteByCourseId(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            jdbcTemplate.update("DELETE FROM semantic_cache");
        } else {
            jdbcTemplate.update("DELETE FROM semantic_cache WHERE course_id = ?", courseId);
        }
    }
}

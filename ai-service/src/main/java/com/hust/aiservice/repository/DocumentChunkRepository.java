package com.hust.aiservice.repository;

import com.hust.aiservice.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {



    @Query(value = "SELECT id, course_id as courseId, lesson_id as lessonId, media_id as mediaId, chunk_index as chunkIndex, content, content_type as contentType, source_citation as sourceCitation, " +
                   "(embedding <=> cast(:queryVector as vector)) as score " +
                   "FROM document_chunks " +
                   "WHERE course_id = :courseId " +
                   "ORDER BY score ASC LIMIT :limit",
           nativeQuery = true)
    java.util.List<Object[]> vectorSearch(
        @Param("courseId") String courseId,
        @Param("queryVector") String queryVectorString,
        @Param("limit") int limit
    );

    @Query(value = "SELECT id, course_id as courseId, lesson_id as lessonId, media_id as mediaId, chunk_index as chunkIndex, content, content_type as contentType, source_citation as sourceCitation, " +
                   "ts_rank_cd(fts_document, websearch_to_tsquery('simple', :queryText)) as score " +
                   "FROM document_chunks " +
                   "WHERE course_id = :courseId " +
                   "AND fts_document @@ websearch_to_tsquery('simple', :queryText) " +
                   "ORDER BY score DESC LIMIT :limit",
           nativeQuery = true)
    java.util.List<Object[]> ftsSearch(
        @Param("courseId") String courseId,
        @Param("queryText") String queryText,
        @Param("limit") int limit
    );

    @Query(value = "SELECT content FROM document_chunks " +
                   "WHERE media_id = :mediaId " +
                   "AND chunk_index BETWEEN :chunkIndex - :windowSize AND :chunkIndex + :windowSize " +
                   "ORDER BY chunk_index ASC",
           nativeQuery = true)
    java.util.List<String> findNeighboringChunks(
        @Param("mediaId") String mediaId,
        @Param("chunkIndex") int chunkIndex,
        @Param("windowSize") int windowSize
    );
}

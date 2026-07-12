-- 1. Kích hoạt extension pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tạo bảng document_chunks lưu trữ RAG chunks
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL,
    lesson_id VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    source_citation VARCHAR(50),
    embedding vector(768)
);

-- 3. Tạo chỉ mục B-Tree trên course_id cho Pre-filtering
CREATE INDEX IF NOT EXISTS document_chunks_course_idx ON document_chunks(course_id);

-- 4. Thêm cột fts_document cho Full-Text Search
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS fts_document tsvector
GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED;

-- 5. Tạo chỉ mục GIN cho Full-Text Search
CREATE INDEX IF NOT EXISTS document_chunks_fts_idx ON document_chunks USING GIN(fts_document);

-- 6. Tạo chỉ mục HNSW cho tìm kiếm vector khoảng cách Cosine
CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx ON document_chunks USING HNSW (embedding vector_cosine_ops);

-- 1. Thêm media_id, chunk_index và created_at vào document_chunks
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS media_id VARCHAR(50);
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS chunk_index INT;
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();
CREATE INDEX IF NOT EXISTS idx_document_chunks_media ON document_chunks(media_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_media_index ON document_chunks(media_id, chunk_index);

-- 2. Thêm prompt_tokens, completion_tokens, total_tokens vào chat_messages
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS prompt_tokens INT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS completion_tokens INT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS total_tokens INT;

-- 3. Thêm hits_count, last_accessed_at, embedding_model vào semantic_cache
ALTER TABLE semantic_cache ADD COLUMN IF NOT EXISTS hits_count INT DEFAULT 0;
ALTER TABLE semantic_cache ADD COLUMN IF NOT EXISTS last_accessed_at TIMESTAMP DEFAULT NOW();
ALTER TABLE semantic_cache ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100);

-- Thêm chỉ mục HNSW cho bảng semantic_cache để tối ưu hóa tìm kiếm vector L2 Cache (Khoảng cách Cosine)
CREATE INDEX IF NOT EXISTS idx_semantic_cache_embedding ON semantic_cache USING HNSW (embedding vector_cosine_ops);

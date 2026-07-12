CREATE TABLE IF NOT EXISTS semantic_cache (
    id BIGSERIAL PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL,
    query TEXT NOT NULL,
    answer TEXT NOT NULL,
    citations JSONB,
    embedding vector(768),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_course ON semantic_cache(course_id);

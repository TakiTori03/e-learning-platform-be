-- Thêm cột routed_agent vào chat_messages để tracking agent nào đã xử lý
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS routed_agent VARCHAR(30);

-- Tạo bảng agent_routing_log để phân tích, debug và monitoring hiệu suất Agent
CREATE TABLE IF NOT EXISTS agent_routing_log (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    user_query TEXT NOT NULL,
    routed_agent VARCHAR(30) NOT NULL,
    confidence_score DOUBLE PRECISION,
    evaluation_score DOUBLE PRECISION,
    was_corrected BOOLEAN DEFAULT FALSE,
    correction_source VARCHAR(50),      -- 'RE_ROUTE', 'WEB_SEARCH', 'QUERY_EXPANSION'
    latency_ms BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_routing_log_session ON agent_routing_log(session_id);
CREATE INDEX IF NOT EXISTS idx_routing_log_agent ON agent_routing_log(routed_agent);
CREATE INDEX IF NOT EXISTS idx_routing_log_created ON agent_routing_log(created_at);

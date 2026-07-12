-- 1. Cho phép course_id nullable trong bảng chat_sessions và semantic_cache
ALTER TABLE chat_sessions ALTER COLUMN course_id DROP NOT NULL;
ALTER TABLE semantic_cache ALTER COLUMN course_id DROP NOT NULL;

-- 2. Bổ sung cột agent_type vào chat_sessions để phân biệt vai trò của phiên chat
ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS agent_type VARCHAR(20) DEFAULT 'TUTOR';

-- 3. Bổ sung user_id vào semantic_cache để phân tách hoặc thu hồi cache khi cần
ALTER TABLE semantic_cache ADD COLUMN IF NOT EXISTS user_id VARCHAR(50);

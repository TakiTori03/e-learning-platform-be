-- Bảng phiên hội thoại
CREATE TABLE IF NOT EXISTS chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    course_id VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Bảng tin nhắn
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL,          -- 'user' hoặc 'assistant'
    content TEXT NOT NULL,
    citations JSONB,                    -- Chỉ cho assistant
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_chat_sessions_user_course ON chat_sessions(user_id, course_id);
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at);

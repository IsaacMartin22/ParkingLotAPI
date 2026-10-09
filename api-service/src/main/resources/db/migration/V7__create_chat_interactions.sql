CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chat_interactions (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    embedding_model VARCHAR(255) NOT NULL,
    chat_model VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

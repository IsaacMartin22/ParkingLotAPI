CREATE TABLE chat_tool_usages (
    id BIGSERIAL PRIMARY KEY,
    chat_interaction_id BIGINT NOT NULL,
    tool_name VARCHAR(255) NOT NULL,
    call_id VARCHAR(255),
    tool_round INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    is_error BOOLEAN NOT NULL,
    error_diagnostic TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE chat_tool_usages
    ADD CONSTRAINT fk_chat_tool_usages_chat_interaction
        FOREIGN KEY (chat_interaction_id) REFERENCES chat_interactions (id) ON DELETE CASCADE;

CREATE INDEX idx_chat_tool_usages_chat_interaction_id ON chat_tool_usages (chat_interaction_id);

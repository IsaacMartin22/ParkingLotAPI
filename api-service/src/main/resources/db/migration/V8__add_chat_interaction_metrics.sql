ALTER TABLE chat_interactions
    ADD COLUMN IF NOT EXISTS cache_hit BOOLEAN,
    ADD COLUMN IF NOT EXISTS embedding_latency_ms BIGINT,
    ADD COLUMN IF NOT EXISTS vector_search_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS vector_search_document_count INTEGER,
    ADD COLUMN IF NOT EXISTS rating INTEGER;

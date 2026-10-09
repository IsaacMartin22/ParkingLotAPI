ALTER TABLE chat_interactions
    DROP COLUMN embedding;

ALTER TABLE chat_interactions
    ADD COLUMN embedding vector(3072) NOT NULL DEFAULT ('[' || array_to_string(array_fill('0'::text, ARRAY[3072]), ',') || ']')::vector;

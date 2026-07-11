CREATE TABLE analytics (
    id BIGSERIAL PRIMARY KEY,
    current_url VARCHAR(255),
    browser VARCHAR(255),
    operating_system VARCHAR(255),
    ip_address VARCHAR(255),
    timestamp TIMESTAMP,
    payload JSONB
);



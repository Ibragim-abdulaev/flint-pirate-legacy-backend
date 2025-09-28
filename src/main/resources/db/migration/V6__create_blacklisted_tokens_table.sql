CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiration TIMESTAMP NOT NULL
);

-- Опционально, индекс для быстрого поиска по дате истечения
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_expiration
ON blacklisted_tokens (expiration);
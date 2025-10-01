CREATE TABLE blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiration TIMESTAMP NOT NULL
);

CREATE INDEX idx_blacklisted_tokens_expiration ON blacklisted_tokens (expiration);
COMMENT ON TABLE blacklisted_tokens IS 'Черный список для вышедших JWT токенов';
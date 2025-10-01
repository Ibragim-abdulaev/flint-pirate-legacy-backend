CREATE TABLE user_popups (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    popup_type VARCHAR(50) NOT NULL,
    is_shown BOOLEAN NOT NULL DEFAULT false,
    shown_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_popups_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_popup_type UNIQUE(user_id, popup_type)
);
COMMENT ON TABLE user_popups IS 'Отслеживание показанных пользователю попапов';
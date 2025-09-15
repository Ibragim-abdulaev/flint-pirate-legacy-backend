-- Создание таблицы для отслеживания показанных попапов
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

-- Создание индексов
CREATE INDEX idx_user_popups_user_id ON user_popups(user_id);
CREATE INDEX idx_user_popups_type_shown ON user_popups(popup_type, is_shown);

-- Комментарии
COMMENT ON TABLE user_popups IS 'Таблица для отслеживания показанных пользователю попапов';
COMMENT ON COLUMN user_popups.popup_type IS 'Тип попапа: WELCOME, FIRST_QUEST и т.д.';
COMMENT ON COLUMN user_popups.is_shown IS 'Флаг - был ли показан попап';
COMMENT ON COLUMN user_popups.shown_at IS 'Время показа попапа';
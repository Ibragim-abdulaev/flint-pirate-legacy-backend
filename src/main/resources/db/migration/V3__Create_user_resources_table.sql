CREATE TABLE user_resources (
    user_id BIGINT PRIMARY KEY,
    gold BIGINT NOT NULL DEFAULT 0,
    wood BIGINT NOT NULL DEFAULT 0,
    stone BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT now(),
    CONSTRAINT fk_resources_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Комментарии к таблице user_resources
COMMENT ON TABLE user_resources IS 'Ресурсы пользователя';
COMMENT ON COLUMN user_resources.gold IS 'Количество золота';
COMMENT ON COLUMN user_resources.wood IS 'Количество дерева';
COMMENT ON COLUMN user_resources.stone IS 'Количество камня';
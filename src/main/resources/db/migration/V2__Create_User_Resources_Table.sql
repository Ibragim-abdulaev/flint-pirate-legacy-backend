CREATE TABLE user_resources (
    user_id BIGINT PRIMARY KEY,
    gold BIGINT NOT NULL DEFAULT 0,
    wood BIGINT NOT NULL DEFAULT 0,
    stone BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_resources_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE user_resources IS 'Ресурсы пользователя (золото, дерево и т.д.)';
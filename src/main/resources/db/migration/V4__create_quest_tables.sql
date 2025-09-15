-- Создание таблицы квестов
CREATE TABLE quests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    npc_name VARCHAR(255) NOT NULL,
    npc_image_url VARCHAR(500),
    gold_reward BIGINT DEFAULT 0,
    exp_reward BIGINT DEFAULT 0,
    button_text VARCHAR(100) DEFAULT 'В путь',
    quest_order INTEGER NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы пользовательских квестов
CREATE TABLE user_quests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_quests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_quests_quest FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_quest UNIQUE(user_id, quest_id)
);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_user_quests_user_id ON user_quests(user_id);
CREATE INDEX idx_user_quests_quest_id ON user_quests(quest_id);
CREATE INDEX idx_user_quests_is_completed ON user_quests(is_completed);
CREATE INDEX idx_user_quests_user_completed ON user_quests(user_id, is_completed);
CREATE INDEX idx_quests_quest_order ON quests(quest_order);
CREATE INDEX idx_quests_is_active ON quests(is_active);
CREATE INDEX idx_quests_active_order ON quests(is_active, quest_order);

-- Комментарии к таблицам
COMMENT ON TABLE quests IS 'Таблица с квестами игры';
COMMENT ON TABLE user_quests IS 'Таблица связи пользователей и квестов';

-- Комментарии к важным колонкам
COMMENT ON COLUMN quests.quest_order IS 'Порядковый номер квеста (уникальный)';
COMMENT ON COLUMN user_quests.is_completed IS 'Флаг завершения квеста пользователем';
COMMENT ON COLUMN user_quests.started_at IS 'Время начала квеста';
COMMENT ON COLUMN user_quests.completed_at IS 'Время завершения квеста';
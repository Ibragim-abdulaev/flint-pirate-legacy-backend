CREATE TABLE quest_chains (
    id BIGSERIAL PRIMARY KEY,
    chain_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    icon_url VARCHAR(500),
    journal_icon_url VARCHAR(500),
    chain_type VARCHAR(50) NOT NULL -- 'STORYLINE' или 'ADVENTURE'
);
COMMENT ON TABLE quest_chains IS 'Квестовые цепочки (сюжетные линии, приключения)';

CREATE TABLE quests (
    id BIGSERIAL PRIMARY KEY,
    quest_chain_id BIGINT NOT NULL,
    quest_key VARCHAR(100) NOT NULL UNIQUE,
    quest_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    npc_name VARCHAR(255),
    npc_image_url VARCHAR(500),
    story_text TEXT,
    objective TEXT,
    gold_reward BIGINT DEFAULT 0,
    exp_reward BIGINT DEFAULT 0,
    wood_reward BIGINT DEFAULT 0,
    stone_reward BIGINT DEFAULT 0,
    crystals_reward BIGINT DEFAULT 0,
    button_text VARCHAR(100) DEFAULT 'В путь',
    battle_location_id VARCHAR(255),
    CONSTRAINT fk_quests_quest_chain FOREIGN KEY (quest_chain_id) REFERENCES quest_chains(id) ON DELETE CASCADE,
    CONSTRAINT uk_quest_chain_order UNIQUE (quest_chain_id, quest_order)
);
COMMENT ON TABLE quests IS 'Конкретные шаги (квесты) внутри квестовых цепочек';

CREATE TABLE quest_item_rewards (
    id BIGSERIAL PRIMARY KEY,
    quest_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_rewards_quest FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    CONSTRAINT fk_rewards_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
COMMENT ON TABLE quest_item_rewards IS 'Связь квестов и их наград в виде предметов';

CREATE TABLE user_quests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_quests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_quests_quest FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_quest UNIQUE(user_id, quest_id)
);
COMMENT ON TABLE user_quests IS 'Прогресс игроков по квестам';
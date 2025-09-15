CREATE TABLE user_heroes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    hero_class VARCHAR(10) NOT NULL,
    character_type VARCHAR(50) NOT NULL,
    min_attack INTEGER NOT NULL DEFAULT 10,
    max_attack INTEGER NOT NULL DEFAULT 15,
    hp INTEGER NOT NULL DEFAULT 100,
    armor INTEGER NOT NULL DEFAULT 0,
    special_ability TEXT,
    owner_id BIGINT NOT NULL,
    CONSTRAINT fk_hero_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Создание индексов для таблицы user_heroes
CREATE INDEX idx_heroes_owner_id ON user_heroes(owner_id);
CREATE INDEX idx_heroes_character_type ON user_heroes(character_type);
CREATE INDEX idx_heroes_hero_class ON user_heroes(hero_class);

-- Комментарии к таблице user_heroes
COMMENT ON TABLE user_heroes IS 'Таблица героев пользователей';
COMMENT ON COLUMN user_heroes.name IS 'Имя героя, выбранное пользователем';
COMMENT ON COLUMN user_heroes.level IS 'Уровень героя';
COMMENT ON COLUMN user_heroes.hero_class IS 'Класс боя: MELEE или RANGED';
COMMENT ON COLUMN user_heroes.character_type IS 'Тип персонажа: BARBARIAN, ARCHER, VALKYRIE и др.';
COMMENT ON COLUMN user_heroes.min_attack IS 'Минимальный урон';
COMMENT ON COLUMN user_heroes.max_attack IS 'Максимальный урон';
COMMENT ON COLUMN user_heroes.hp IS 'Здоровье героя';
COMMENT ON COLUMN user_heroes.armor IS 'Броня героя';
COMMENT ON COLUMN user_heroes.special_ability IS 'Описание спецспособности героя';
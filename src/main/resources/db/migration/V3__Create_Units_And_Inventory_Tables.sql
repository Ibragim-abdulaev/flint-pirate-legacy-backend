-- Справочник всех возможных предметов в игре
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    item_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    item_type VARCHAR(50) NOT NULL, -- WEAPON, ARMOR
    bonus_hp INT NOT NULL DEFAULT 0,
    bonus_damage INT NOT NULL DEFAULT 0,
    bonus_armor INT NOT NULL DEFAULT 0
);
COMMENT ON TABLE items IS 'Справочник всех предметов в игре';

-- Таблица инвентаря пользователей
CREATE TABLE user_inventory (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_inventory_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
COMMENT ON TABLE user_inventory IS 'Предметы в инвентаре пользователей';

-- Таблица для всех юнитов, принадлежащих игрокам (включая главного героя)
CREATE TABLE user_units (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    unit_type_key VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    experience BIGINT NOT NULL DEFAULT 0,
    base_hp INTEGER NOT NULL,
    base_min_attack INTEGER NOT NULL,
    base_max_attack INTEGER NOT NULL,
    base_armor INTEGER NOT NULL,
    is_main_hero BOOLEAN NOT NULL DEFAULT false,
    equipped_weapon_id BIGINT,
    equipped_armor_id BIGINT,
    CONSTRAINT fk_unit_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_unit_weapon FOREIGN KEY (equipped_weapon_id) REFERENCES user_inventory(id),
    CONSTRAINT fk_unit_armor FOREIGN KEY (equipped_armor_id) REFERENCES user_inventory(id)
);
COMMENT ON TABLE user_units IS 'Все юниты игроков (герои и наёмники)';
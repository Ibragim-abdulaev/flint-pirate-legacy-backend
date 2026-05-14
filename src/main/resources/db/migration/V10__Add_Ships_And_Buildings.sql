-- Корабли игрока
CREATE TABLE user_ships (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ship_type_key   VARCHAR(50) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    level           INTEGER NOT NULL DEFAULT 1,
    capacity        INTEGER NOT NULL,
    mode            VARCHAR(20) NOT NULL DEFAULT 'RESERVE',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON COLUMN user_ships.mode IS 'Режим корабля: ATTACK, DEFENSE, RESERVE';

-- Юнит привязан к кораблю (NULL = свободный)
ALTER TABLE user_units
    ADD COLUMN ship_id BIGINT REFERENCES user_ships(id) ON DELETE SET NULL;

-- Здания игрока
CREATE TABLE user_buildings (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    building_type   VARCHAR(50) NOT NULL,
    level           INTEGER NOT NULL DEFAULT 1,
    UNIQUE(owner_id, building_type)
);

COMMENT ON COLUMN user_buildings.building_type IS 'Тип здания: TAVERN, PORT, ARMORY';
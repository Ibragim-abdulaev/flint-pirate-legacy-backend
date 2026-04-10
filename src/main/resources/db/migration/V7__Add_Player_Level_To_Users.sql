ALTER TABLE users
    ADD COLUMN player_level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN player_exp BIGINT  NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.player_level IS 'Общий уровень аккаунта игрока (не сбрасывается)';
COMMENT ON COLUMN users.player_exp   IS 'Накопленный опыт аккаунта (суммарный, не вычитается при level up)';
ALTER TABLE users
    ADD COLUMN player_level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN player_exp   BIGINT  NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.player_level IS 'Уровень острова (не сбрасывается)';
COMMENT ON COLUMN users.player_exp   IS 'Суммарный накопленный опыт острова';
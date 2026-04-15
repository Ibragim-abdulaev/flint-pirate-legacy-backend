ALTER TABLE user_units
    ADD COLUMN is_alive BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN recovery_ends_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

COMMENT ON COLUMN user_units.is_alive IS 'Жив ли юнит. false = на восстановлении';
COMMENT ON COLUMN user_units.recovery_ends_at IS 'Время окончания восстановления. Только для главного героя, для юнитов NULL';
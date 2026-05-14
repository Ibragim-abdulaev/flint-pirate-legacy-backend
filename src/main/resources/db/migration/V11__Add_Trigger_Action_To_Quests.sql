ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS trigger_action VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN quests.trigger_action IS 'Действие триггерящее прогресс квеста (HIRE_UNIT, BUILD_SHIP и т.д.). NULL = завершается вручную';
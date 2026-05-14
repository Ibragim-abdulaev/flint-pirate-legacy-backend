-- Счётчик прогресса квеста (например сколько пиратов куплено)
ALTER TABLE user_quests
    ADD COLUMN progress INTEGER NOT NULL DEFAULT 0;

-- Сколько действий нужно для завершения квеста (default = 1)
ALTER TABLE quests
    ADD COLUMN required_count INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN user_quests.progress      IS 'Сколько раз выполнено действие по квесту';
COMMENT ON COLUMN quests.required_count     IS 'Сколько раз нужно выполнить действие для завершения квеста';

-- Действие которое триггерит прогресс квеста
ALTER TABLE quests
    ADD COLUMN trigger_action VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN quests.trigger_action IS 'Действие триггерящее прогресс квеста (HIRE_UNIT, BUILD_SHIP и т.д.). NULL = завершается вручную';
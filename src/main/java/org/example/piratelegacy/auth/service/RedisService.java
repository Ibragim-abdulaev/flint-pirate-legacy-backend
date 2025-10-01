package org.example.piratelegacy.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Удобный сервис-обёртка для выполнения общих операций с Redis.
 * Скрывает детали работы с RedisTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Сохраняет значение в Redis с указанным временем жизни (TTL).
     *
     * @param key   Ключ, по которому будет сохранено значение.
     * @param value Объект для сохранения. Он будет автоматически преобразован в JSON.
     * @param ttl   Время жизни записи (например, Duration.ofMinutes(10)).
     */
    public <T> void set(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Получает значение из Redis по ключу, безопасно приводя его к нужному типу.
     *
     * @param key   Ключ для поиска.
     * @param clazz Класс, к которому нужно привести результат (например, String.class или BattleLocationDto.class).
     * @return Объект нужного типа или null, если ключ не найден или тип не совпадает.
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    /**
     * Проверяет, существует ли ключ в Redis.
     * Это эффективнее, чем вызывать get() и проверять на null.
     *
     * @param key Ключ для проверки.
     * @return true, если ключ существует, иначе false.
     */
    public boolean hasKey(String key) {
        // Boolean.TRUE.equals() - это безопасный способ работы с результатом, который может быть null
        return redisTemplate.hasKey(key);
    }

    /**
     * Удаляет запись из Redis по ключу.
     *
     * @param key Ключ для удаления.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
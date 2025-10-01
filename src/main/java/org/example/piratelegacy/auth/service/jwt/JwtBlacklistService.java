package org.example.piratelegacy.auth.service.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.service.RedisService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final JwtService jwtService;
    private final RedisService redisService;
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    /**
     * Добавляет токен в черный список в Redis.
     * Время жизни записи будет равно оставшемуся времени жизни самого токена.
     */
    public void addToBlacklist(String token) {
        Date expiration = jwtService.extractExpiration(token);
        long remainingValidity = expiration.getTime() - System.currentTimeMillis();

        if (remainingValidity > 0) {
            String key = BLACKLIST_KEY_PREFIX + token;
            redisService.set(key, "blacklisted", Duration.ofMillis(remainingValidity));
        }
    }

    /**
     * Проверяет, находится ли токен в черном списке Redis.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        // hasKey() - очень быстрая операция в Redis
        return redisService.hasKey(key);
    }
}
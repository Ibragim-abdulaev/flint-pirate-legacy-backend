package org.example.piratelegacy.auth.service.jwt;

import lombok.RequiredArgsConstructor;
import org.example.piratelegacy.auth.entity.BlacklistedToken;
import org.example.piratelegacy.auth.repository.BlacklistedTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final JwtService jwtService;
    private final BlacklistedTokenRepository repository;

    public void addToBlacklist(String token) {
        Date expiration = jwtService.extractExpiration(token);
        repository.findByToken(token)
                .orElseGet(() -> repository.save(new BlacklistedToken(token, expiration)));
    }

    public boolean isBlacklisted(String token) {
        return repository.findByToken(token).isPresent();
    }

    // чистим раз в час (можно чаще/реже)
    @Transactional
    @Scheduled(fixedRate = 3600_000)
    public void cleanupExpiredTokens() {
        repository.deleteByExpirationBefore(new Date());
    }
}


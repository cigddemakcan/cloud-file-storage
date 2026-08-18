package com.example.filestorage.service;

import com.example.filestorage.entity.AuditAction;
import com.example.filestorage.entity.RefreshToken;
import com.example.filestorage.entity.User;
import com.example.filestorage.exception.InvalidRefreshTokenException;
import com.example.filestorage.exception.RefreshTokenReuseDetectedException;
import com.example.filestorage.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;


@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuditLogService auditLogService,
            @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.refreshExpirationMs = refreshExpirationMs;
    }


    public record IssuedRefreshToken(String rawToken, User user) {
    }

    @Transactional
    public IssuedRefreshToken createFor(User user) {
        return createInFamily(user, UUID.randomUUID().toString());
    }

    private IssuedRefreshToken createInFamily(User user, String family) {
        String rawToken = generateRawToken();

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(rawToken));
        entity.setTokenFamily(family);
        entity.setUser(user);
        entity.setExpiresAt(Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));

        refreshTokenRepository.save(entity);

        return new IssuedRefreshToken(rawToken, user);
    }

    @Transactional
    public IssuedRefreshToken validateAndRotate(String rawToken) {
        String tokenHash = hash(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked()) {

            log.warn("Refresh token reuse tespit edildi - family iptal ediliyor: userId={}, family={}",
                    existing.getUser().getId(), existing.getTokenFamily());

            refreshTokenRepository.revokeAllInFamily(existing.getTokenFamily());

            auditLogService.log(existing.getUser().getId(), AuditAction.TOKEN_REUSE_DETECTED,
                    existing.getId(), null, false,
                    "family=" + existing.getTokenFamily());

            throw new RefreshTokenReuseDetectedException();
        }

        if (!existing.isUsable()) {
            throw new InvalidRefreshTokenException();
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createInFamily(existing.getUser(), existing.getTokenFamily());
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }


    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException("SHA-256 algoritmasi bulunamadi", e);
        }
    }
}

package com.example.filestorage.security;

import com.example.filestorage.exception.TooManyAttemptsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final Duration STALE_ENTRY_TTL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();


    public void assertNotBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null || record.lockedUntil == null) {
            return;
        }

        Instant now = Instant.now();
        if (now.isAfter(record.lockedUntil)) {
            attempts.remove(key);
            return;
        }

        Duration remaining = Duration.between(now, record.lockedUntil);
        throw new TooManyAttemptsException(remaining);
    }

    public void recordFailure(String key) {
        AttemptRecord record = attempts.computeIfAbsent(key, k -> new AttemptRecord());
        record.lastFailureAt = Instant.now();
        int count = record.failureCount.incrementAndGet();

        if (count >= MAX_ATTEMPTS) {
            record.lockedUntil = Instant.now().plus(LOCK_DURATION);
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }


    @Scheduled(fixedRate = 600_000)
    void cleanupStaleEntries() {
        Instant now = Instant.now();
        int before = attempts.size();

        attempts.entrySet().removeIf(entry -> {
            AttemptRecord record = entry.getValue();
            if (record.lockedUntil != null) {
                return now.isAfter(record.lockedUntil);
            }
            return record.lastFailureAt != null
                    && now.isAfter(record.lastFailureAt.plus(STALE_ENTRY_TTL));
        });

        int removed = before - attempts.size();
        if (removed > 0) {
            log.debug("LoginAttemptService: {} eski kayit temizlendi", removed);
        }
    }

    private static class AttemptRecord {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile Instant lockedUntil;
        private volatile Instant lastFailureAt;
    }
}

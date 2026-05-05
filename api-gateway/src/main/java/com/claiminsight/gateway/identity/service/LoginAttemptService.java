package com.claiminsight.gateway.identity.service;

import com.claiminsight.gateway.exception.AccountLockedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the login endpoint.
 *
 * After MAX_ATTEMPTS consecutive failures for a given identifier (username
 * or email, lower-cased), the account is locked for LOCK_DURATION_MS ms.
 * A successful login resets the counter immediately.
 *
 * State is in-memory — a service restart clears all lockouts, which is
 * acceptable for a dev deployment. For production, back this with Redis.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int  MAX_ATTEMPTS     = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1_000L; // 15 minutes

    private record Attempt(int count, long lockUntilMs) {}

    private final ConcurrentHashMap<String, Attempt> cache = new ConcurrentHashMap<>();

    /**
     * Must be called BEFORE the password check.
     * Throws {@link AccountLockedException} if the identifier is currently locked.
     */
    public void checkLocked(String identifier) {
        Attempt a = cache.get(normalise(identifier));
        if (a != null && a.lockUntilMs() > System.currentTimeMillis()) {
            long secsLeft = (a.lockUntilMs() - System.currentTimeMillis()) / 1_000;
            log.warn("[auth] login blocked for '{}' — {} s remaining", identifier, secsLeft);
            throw new AccountLockedException(
                    "Too many failed attempts. Account locked for "
                    + Math.max(1, secsLeft / 60) + " more minute(s).");
        }
    }

    /** Call on every failed password check or user-not-found. */
    public void recordFailure(String identifier) {
        String key = normalise(identifier);
        Attempt prev = cache.getOrDefault(key, new Attempt(0, 0));
        int next = prev.count() + 1;
        long lockUntil = (next >= MAX_ATTEMPTS)
                ? System.currentTimeMillis() + LOCK_DURATION_MS
                : 0L;
        cache.put(key, new Attempt(next, lockUntil));
        if (next >= MAX_ATTEMPTS) {
            log.warn("[auth] '{}' locked after {} failed attempts", identifier, next);
        }
    }

    /** Call after a successful authentication — resets the counter. */
    public void recordSuccess(String identifier) {
        cache.remove(normalise(identifier));
    }

    private String normalise(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }
}

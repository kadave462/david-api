package com.example.david_api.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// The dashboard has exactly one shared account, publicly reachable — an
// attacker gets unlimited free guesses at ADMIN_PASSWORD unless something
// throttles them. Tracks failed attempts per client IP in memory (this is a
// single-instance deployment, so no shared cache is needed) and locks an IP
// out for LOCKOUT_DURATION after MAX_ATTEMPTS consecutive failures.
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private record Attempts(int count, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    public boolean isLockedOut(String ip) {
        Attempts a = attemptsByIp.get(ip);
        return a != null && a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil());
    }

    public void recordFailure(String ip) {
        attemptsByIp.compute(ip, (key, existing) -> {
            int count = (existing == null ? 0 : existing.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCKOUT_DURATION) : null;
            return new Attempts(count, lockedUntil);
        });
    }

    public void recordSuccess(String ip) {
        attemptsByIp.remove(ip);
    }
}

package com.pablohenrique.workflowengine.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login por origem (janela deslizante). Complementa o bloqueio por conta: este impede
 * que uma mesma origem teste muitas contas; o bloqueio impede muitas senhas contra uma mesma conta.
 *
 * <p>O estado é local à instância. Com várias instâncias, o limite efetivo é multiplicado; em produção o
 * gateway deve aplicar o seu próprio limite (docs/architecture/SECURITY.md).
 */
public class LoginThrottle {

    private static final int MAX_TRACKED_ORIGINS = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attemptsByOrigin = new ConcurrentHashMap<>();

    public LoginThrottle(int maxAttempts, Duration window, Clock clock) {
        if (maxAttempts < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("maxAttempts must be positive and window must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Registra a tentativa da origem.
     *
     * @throws TooManyLoginAttemptsException quando a origem excedeu o limite da janela
     */
    public void acquire(String origin) {
        Instant now = clock.instant();
        if (attemptsByOrigin.size() > MAX_TRACKED_ORIGINS) {
            evictExpired(now);
        }
        Deque<Instant> attempts = attemptsByOrigin.computeIfAbsent(origin, key -> new ArrayDeque<>());
        synchronized (attempts) {
            discardOlderThanWindow(attempts, now);
            if (attempts.size() >= maxAttempts) {
                Duration retryAfter = Duration.between(now, attempts.peekFirst().plus(window));
                throw new TooManyLoginAttemptsException(Math.max(1, retryAfter.toSeconds()));
            }
            attempts.addLast(now);
        }
    }

    private void discardOlderThanWindow(Deque<Instant> attempts, Instant now) {
        Instant limit = now.minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(limit)) {
            attempts.pollFirst();
        }
    }

    private void evictExpired(Instant now) {
        attemptsByOrigin.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                discardOlderThanWindow(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }
}

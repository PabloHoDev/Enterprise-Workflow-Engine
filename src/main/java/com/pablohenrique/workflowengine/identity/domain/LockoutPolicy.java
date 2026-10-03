package com.pablohenrique.workflowengine.identity.domain;

import java.time.Duration;

/**
 * Bloqueio temporário de conta após senhas erradas consecutivas (proteção contra força bruta).
 */
public record LockoutPolicy(int maxFailedAttempts, Duration lockDuration) {

    public LockoutPolicy {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts must be at least 1");
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("lockDuration must be positive");
        }
    }
}

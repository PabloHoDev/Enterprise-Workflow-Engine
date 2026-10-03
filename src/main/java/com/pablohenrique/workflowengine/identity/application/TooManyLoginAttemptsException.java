package com.pablohenrique.workflowengine.identity.application;

import com.pablohenrique.workflowengine.identity.domain.IdentityException;

public class TooManyLoginAttemptsException extends IdentityException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("Too many login attempts. Try again in " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}

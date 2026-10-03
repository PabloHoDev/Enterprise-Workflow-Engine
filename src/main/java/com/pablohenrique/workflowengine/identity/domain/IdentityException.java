package com.pablohenrique.workflowengine.identity.domain;

public abstract class IdentityException extends RuntimeException {

    protected IdentityException(String message) {
        super(message);
    }
}

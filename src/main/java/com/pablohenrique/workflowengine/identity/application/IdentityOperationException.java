package com.pablohenrique.workflowengine.identity.application;

import com.pablohenrique.workflowengine.identity.domain.IdentityException;

/** Erros de caso de uso do módulo Identity, cada um com o seu motivo. */
public class IdentityOperationException extends IdentityException {

    public enum Reason {
        USER_NOT_FOUND,
        USERNAME_TAKEN,
        LAST_ADMINISTRATOR,
        SELF_LOCKOUT,
        WRONG_CURRENT_PASSWORD
    }

    private final Reason reason;

    public IdentityOperationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    static IdentityOperationException userNotFound(String username) {
        return new IdentityOperationException(Reason.USER_NOT_FOUND, "User '" + username + "' was not found");
    }
}

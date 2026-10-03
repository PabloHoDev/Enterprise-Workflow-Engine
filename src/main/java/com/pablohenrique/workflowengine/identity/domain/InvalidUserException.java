package com.pablohenrique.workflowengine.identity.domain;

import java.util.List;

public class InvalidUserException extends IdentityException {

    private final List<String> violations;

    public InvalidUserException(List<String> violations) {
        super("User data is invalid: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}

package com.pablohenrique.workflowengine.identity.domain;

import java.util.List;

public class WeakPasswordException extends IdentityException {

    private final List<String> violations;

    public WeakPasswordException(List<String> violations) {
        super("Password does not meet the password policy");
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}

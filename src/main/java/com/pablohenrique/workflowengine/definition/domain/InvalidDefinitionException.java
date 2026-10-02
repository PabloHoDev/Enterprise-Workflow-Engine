package com.pablohenrique.workflowengine.definition.domain;

import java.util.List;

public class InvalidDefinitionException extends DefinitionException {

    private final List<String> violations;

    public InvalidDefinitionException(List<String> violations) {
        super("Workflow definition is invalid: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}

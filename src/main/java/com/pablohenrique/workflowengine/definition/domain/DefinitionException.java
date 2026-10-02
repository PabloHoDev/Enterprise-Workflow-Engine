package com.pablohenrique.workflowengine.definition.domain;

public abstract class DefinitionException extends RuntimeException {

    protected DefinitionException(String message) {
        super(message);
    }
}

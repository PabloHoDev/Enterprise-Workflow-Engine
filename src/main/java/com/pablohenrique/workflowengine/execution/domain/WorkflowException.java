package com.pablohenrique.workflowengine.execution.domain;

public abstract class WorkflowException extends RuntimeException {

    protected WorkflowException(String message) {
        super(message);
    }
}

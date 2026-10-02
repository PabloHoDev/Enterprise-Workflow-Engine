package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.execution.domain.WorkflowException;

import java.util.UUID;

public class WorkflowNotFoundException extends WorkflowException {

    public WorkflowNotFoundException(UUID id) {
        super("Workflow " + id + " was not found");
    }
}

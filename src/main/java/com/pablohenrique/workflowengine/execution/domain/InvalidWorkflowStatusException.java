package com.pablohenrique.workflowengine.execution.domain;

import java.util.UUID;

/** A operação é incompatível com o ponto do ciclo de vida em que o Workflow está (BR-039). */
public class InvalidWorkflowStatusException extends WorkflowException {

    public InvalidWorkflowStatusException(UUID workflowId, WorkflowStatus current, String operation) {
        super("Workflow " + workflowId + " cannot " + operation + " because it is " + current);
    }
}

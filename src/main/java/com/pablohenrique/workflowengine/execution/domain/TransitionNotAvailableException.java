package com.pablohenrique.workflowengine.execution.domain;

import java.util.UUID;

/** A ação não corresponde a nenhuma Transition definida a partir do State atual (BR-017, BR-018). */
public class TransitionNotAvailableException extends WorkflowException {

    public TransitionNotAvailableException(UUID workflowId, String currentState, String action) {
        super("Action '" + action + "' is not available for workflow " + workflowId
                + " in state '" + currentState + "'");
    }
}

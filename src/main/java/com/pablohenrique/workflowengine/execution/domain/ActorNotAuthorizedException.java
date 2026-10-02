package com.pablohenrique.workflowengine.execution.domain;

/** O Actor está identificado, mas não possui o papel exigido pela Transition (BR-020, BR-027). */
public class ActorNotAuthorizedException extends WorkflowException {

    public ActorNotAuthorizedException(String actorId, String action, String requiredRole) {
        super("Actor '" + actorId + "' is not authorized to execute action '" + action
                + "': role '" + requiredRole + "' is required");
    }
}

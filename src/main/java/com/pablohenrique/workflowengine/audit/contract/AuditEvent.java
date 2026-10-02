package com.pablohenrique.workflowengine.audit.contract;

import java.util.Objects;

/**
 * Operação relevante a ser registrada: quem fez o quê, sobre qual recurso e com qual resultado (BR-033).
 * O momento da operação é atribuído pelo módulo Audit ao registrar.
 */
public record AuditEvent(String actorId, String operation, String resourceType, String resourceId,
                         AuditOutcome outcome, String detail) {

    public AuditEvent {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(outcome, "outcome");
    }

    public static AuditEvent success(String actorId, String operation, String resourceType, String resourceId,
                                     String detail) {
        return new AuditEvent(actorId, operation, resourceType, resourceId, AuditOutcome.SUCCESS, detail);
    }

    public static AuditEvent rejected(String actorId, String operation, String resourceType, String resourceId,
                                      String detail) {
        return new AuditEvent(actorId, operation, resourceType, resourceId, AuditOutcome.REJECTED, detail);
    }
}

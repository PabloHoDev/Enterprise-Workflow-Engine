package com.pablohenrique.workflowengine.audit.application;

import com.pablohenrique.workflowengine.audit.contract.AuditOutcome;

/** Filtros opcionais: campos {@code null} não restringem a busca. */
public record AuditSearchCriteria(String actorId, String operation, String resourceType, String resourceId,
                                  AuditOutcome outcome) {
}

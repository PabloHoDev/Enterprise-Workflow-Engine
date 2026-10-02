package com.pablohenrique.workflowengine.audit.domain;

import com.pablohenrique.workflowengine.audit.contract.AuditOutcome;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutável de uma operação: quem fez o quê, quando e qual foi o resultado.
 */
public record AuditRecord(UUID id, String actorId, String operation, String resourceType, String resourceId,
                          AuditOutcome outcome, String detail, Instant occurredAt) {

    public static final int MAX_DETAIL_LENGTH = 1000;

    public AuditRecord {
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            detail = detail.substring(0, MAX_DETAIL_LENGTH);
        }
    }
}

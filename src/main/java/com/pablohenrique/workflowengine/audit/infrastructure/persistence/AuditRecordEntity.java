package com.pablohenrique.workflowengine.audit.infrastructure.persistence;

import com.pablohenrique.workflowengine.audit.contract.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_record")
class AuditRecordEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private String actorId;

    @Column(nullable = false, updatable = false)
    private String operation;

    @Column(name = "resource_type", nullable = false, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditOutcome outcome;

    @Column(updatable = false)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    // Registros de auditoria só são inseridos: com o id atribuído, evita o SELECT que o merge faria.
    @Transient
    private boolean isNew = true;

    protected AuditRecordEntity() {
    }

    AuditRecordEntity(UUID id, String actorId, String operation, String resourceType, String resourceId,
                      AuditOutcome outcome, String detail, Instant occurredAt) {
        this.id = id;
        this.actorId = actorId;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    String getActorId() {
        return actorId;
    }

    String getOperation() {
        return operation;
    }

    String getResourceType() {
        return resourceType;
    }

    String getResourceId() {
        return resourceId;
    }

    AuditOutcome getOutcome() {
        return outcome;
    }

    String getDetail() {
        return detail;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }
}

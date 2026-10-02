package com.pablohenrique.workflowengine.execution.infrastructure.persistence;

import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "workflow")
class WorkflowEntity {

    @Id
    private UUID id;

    @Column(name = "definition_key", nullable = false, updatable = false)
    private String definitionKey;

    @Column(name = "definition_version_id", nullable = false, updatable = false)
    private UUID definitionVersionId;

    @Column(name = "definition_version_number", nullable = false, updatable = false)
    private int definitionVersionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(name = "current_state", nullable = false)
    private String currentState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> variables;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Duas operações concorrentes sobre o mesmo Workflow: a segunda falha em vez de sobrescrever (RNF-001).
    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<WorkflowHistoryEntity> history = new ArrayList<>();

    protected WorkflowEntity() {
    }

    WorkflowEntity(UUID id, String definitionKey, UUID definitionVersionId, int definitionVersionNumber,
                   WorkflowStatus status, String currentState, Map<String, Object> variables, Instant createdAt,
                   Instant updatedAt) {
        this.id = id;
        this.definitionKey = definitionKey;
        this.definitionVersionId = definitionVersionId;
        this.definitionVersionNumber = definitionVersionNumber;
        this.status = status;
        this.currentState = currentState;
        this.variables = variables;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void update(WorkflowStatus status, String currentState, Map<String, Object> variables, Instant updatedAt) {
        this.status = status;
        this.currentState = currentState;
        this.variables = variables;
        this.updatedAt = updatedAt;
    }

    void addHistory(WorkflowHistoryEntity entry) {
        entry.setWorkflow(this);
        history.add(entry);
    }

    UUID getId() {
        return id;
    }

    String getDefinitionKey() {
        return definitionKey;
    }

    UUID getDefinitionVersionId() {
        return definitionVersionId;
    }

    int getDefinitionVersionNumber() {
        return definitionVersionNumber;
    }

    WorkflowStatus getStatus() {
        return status;
    }

    String getCurrentState() {
        return currentState;
    }

    Map<String, Object> getVariables() {
        return variables;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    List<WorkflowHistoryEntity> getHistory() {
        return history;
    }
}

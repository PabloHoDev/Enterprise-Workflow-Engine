package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import com.pablohenrique.workflowengine.definition.domain.VersionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_definition_version")
class DefinitionVersionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false, updatable = false)
    private WorkflowDefinitionEntity definition;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "workflow_state", joinColumns = @JoinColumn(name = "version_id"))
    @OrderColumn(name = "ordinal")
    private List<StateEmbeddable> states = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TransitionEntity> transitions = new ArrayList<>();

    protected DefinitionVersionEntity() {
    }

    DefinitionVersionEntity(UUID id, int number, VersionStatus status, Instant createdAt,
                            List<StateEmbeddable> states, List<TransitionEntity> transitions) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.createdAt = createdAt;
        this.states.addAll(states);
        transitions.forEach(transition -> {
            transition.setVersion(this);
            this.transitions.add(transition);
        });
    }

    void setDefinition(WorkflowDefinitionEntity definition) {
        this.definition = definition;
    }

    void setStatus(VersionStatus status) {
        this.status = status;
    }

    UUID getId() {
        return id;
    }

    WorkflowDefinitionEntity getDefinition() {
        return definition;
    }

    int getNumber() {
        return number;
    }

    VersionStatus getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    List<StateEmbeddable> getStates() {
        return states;
    }

    List<TransitionEntity> getTransitions() {
        return transitions;
    }
}

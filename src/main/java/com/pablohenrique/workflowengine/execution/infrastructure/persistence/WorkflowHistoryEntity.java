package com.pablohenrique.workflowengine.execution.infrastructure.persistence;

import com.pablohenrique.workflowengine.execution.domain.HistoryEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_history")
class WorkflowHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, updatable = false)
    private WorkflowEntity workflow;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    private HistoryEventType type;

    @Column(updatable = false)
    private String action;

    @Column(name = "from_state", updatable = false)
    private String fromState;

    @Column(name = "to_state", nullable = false, updatable = false)
    private String toState;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private String actorId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "comment_text", updatable = false)
    private String comment;

    protected WorkflowHistoryEntity() {
    }

    WorkflowHistoryEntity(int sequence, HistoryEventType type, String action, String fromState, String toState,
                          String actorId, Instant occurredAt, String comment) {
        this.sequence = sequence;
        this.type = type;
        this.action = action;
        this.fromState = fromState;
        this.toState = toState;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.comment = comment;
    }

    void setWorkflow(WorkflowEntity workflow) {
        this.workflow = workflow;
    }

    int getSequence() {
        return sequence;
    }

    HistoryEventType getType() {
        return type;
    }

    String getAction() {
        return action;
    }

    String getFromState() {
        return fromState;
    }

    String getToState() {
        return toState;
    }

    String getActorId() {
        return actorId;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    String getComment() {
        return comment;
    }
}

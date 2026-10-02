package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_transition")
class TransitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private DefinitionVersionEntity version;

    @Column(name = "ordinal", nullable = false)
    private int position;

    @Column(nullable = false)
    private String action;

    @Column(name = "from_state", nullable = false)
    private String fromState;

    @Column(name = "to_state", nullable = false)
    private String toState;

    @Column(name = "required_role")
    private String requiredRole;

    @ElementCollection
    @CollectionTable(name = "workflow_transition_rule", joinColumns = @JoinColumn(name = "transition_id"))
    @OrderColumn(name = "ordinal")
    private List<RuleEmbeddable> rules = new ArrayList<>();

    protected TransitionEntity() {
    }

    TransitionEntity(int position, String action, String fromState, String toState, String requiredRole,
                     List<RuleEmbeddable> rules) {
        this.position = position;
        this.action = action;
        this.fromState = fromState;
        this.toState = toState;
        this.requiredRole = requiredRole;
        this.rules.addAll(rules);
    }

    void setVersion(DefinitionVersionEntity version) {
        this.version = version;
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

    String getRequiredRole() {
        return requiredRole;
    }

    List<RuleEmbeddable> getRules() {
        return rules;
    }
}

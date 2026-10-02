package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import com.pablohenrique.workflowengine.definition.domain.StateType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
class StateEmbeddable {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_type", nullable = false)
    private StateType type;

    protected StateEmbeddable() {
    }

    StateEmbeddable(String name, StateType type) {
        this.name = name;
        this.type = type;
    }

    String getName() {
        return name;
    }

    StateType getType() {
        return type;
    }
}

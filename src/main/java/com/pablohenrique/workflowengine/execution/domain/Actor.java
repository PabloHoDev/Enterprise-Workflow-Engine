package com.pablohenrique.workflowengine.execution.domain;

import java.util.Set;

/**
 * Responsável por uma ação: usuário, sistema externo ou processo automático (BR-026).
 */
public record Actor(String id, Set<String> roles) {

    public Actor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Actor id must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}

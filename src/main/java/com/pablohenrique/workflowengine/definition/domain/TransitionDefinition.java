package com.pablohenrique.workflowengine.definition.domain;

import com.pablohenrique.workflowengine.rules.contract.Rule;

import java.util.List;

/**
 * Movimento permitido entre dois States, disparado por uma ação.
 *
 * @param requiredRole papel exigido do Actor (BR-020); {@code null} quando qualquer Actor autenticado pode executar
 * @param rules        condições que devem ser todas satisfeitas antes da efetivação (BR-021)
 */
public record TransitionDefinition(String action, String from, String to, String requiredRole, List<Rule> rules) {

    public TransitionDefinition {
        rules = rules == null ? List.of() : List.copyOf(rules);
        requiredRole = requiredRole == null || requiredRole.isBlank() ? null : requiredRole.trim();
    }
}

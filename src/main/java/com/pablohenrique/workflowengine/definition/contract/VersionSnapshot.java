package com.pablohenrique.workflowengine.definition.contract;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Visão imutável de uma versão de Workflow Definition, exposta aos demais módulos.
 */
public record VersionSnapshot(UUID versionId, String definitionKey, int versionNumber, String initialState,
                              Set<String> terminalStates, List<TransitionSnapshot> transitions) {

    public VersionSnapshot {
        terminalStates = Set.copyOf(terminalStates);
        transitions = List.copyOf(transitions);
    }

    public boolean isTerminal(String state) {
        return terminalStates.contains(state);
    }

    public Optional<TransitionSnapshot> transition(String fromState, String action) {
        return transitions.stream()
                .filter(transition -> transition.from().equals(fromState) && transition.action().equals(action))
                .findFirst();
    }

    public List<TransitionSnapshot> transitionsFrom(String state) {
        return transitions.stream().filter(transition -> transition.from().equals(state)).toList();
    }
}

package com.pablohenrique.workflowengine.definition.domain;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Versão de uma Workflow Definition. A estrutura (States e Transitions) é imutável desde a criação
 * (BR-006, BR-007): apenas o status muda ao longo da vida da versão.
 */
public final class DefinitionVersion {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

    private final UUID id;
    private final int number;
    private final List<StateDefinition> states;
    private final List<TransitionDefinition> transitions;
    private final Instant createdAt;
    private VersionStatus status;

    private DefinitionVersion(UUID id, int number, VersionStatus status, List<StateDefinition> states,
                              List<TransitionDefinition> transitions, Instant createdAt) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.states = List.copyOf(states);
        this.transitions = List.copyOf(transitions);
        this.createdAt = createdAt;
    }

    static DefinitionVersion create(int number, List<StateDefinition> states,
                                    List<TransitionDefinition> transitions, Instant now) {
        List<StateDefinition> safeStates = states == null ? List.of() : states;
        List<TransitionDefinition> safeTransitions = transitions == null ? List.of() : transitions;
        List<String> violations = validate(safeStates, safeTransitions);
        if (!violations.isEmpty()) {
            throw new InvalidDefinitionException(violations);
        }
        return new DefinitionVersion(UUID.randomUUID(), number, VersionStatus.DRAFT, safeStates, safeTransitions, now);
    }

    /** Reconstitui uma versão já validada a partir do mecanismo de persistência. */
    public static DefinitionVersion restore(UUID id, int number, VersionStatus status, List<StateDefinition> states,
                                            List<TransitionDefinition> transitions, Instant createdAt) {
        return new DefinitionVersion(id, number, status, states, transitions, createdAt);
    }

    private static List<String> validate(List<StateDefinition> states, List<TransitionDefinition> transitions) {
        List<String> violations = new ArrayList<>();

        if (states.isEmpty()) {
            violations.add("at least one state is required");
            return violations;
        }

        Set<String> stateNames = new HashSet<>();
        for (StateDefinition state : states) {
            if (state.name() == null || !IDENTIFIER.matcher(state.name()).matches()) {
                violations.add("state name '" + state.name() + "' is invalid");
            } else if (!stateNames.add(state.name())) {
                violations.add("state '" + state.name() + "' is duplicated");
            }
            if (state.type() == null) {
                violations.add("state '" + state.name() + "' has no type");
            }
        }

        List<String> initialStates = namesOfType(states, StateType.INITIAL);
        if (initialStates.size() != 1) {
            violations.add("exactly one INITIAL state is required, found " + initialStates.size());
        }
        Set<String> terminalStates = Set.copyOf(namesOfType(states, StateType.TERMINAL));
        if (terminalStates.isEmpty()) {
            violations.add("at least one TERMINAL state is required");
        }

        Set<String> transitionKeys = new HashSet<>();
        Set<String> statesWithOutgoing = new HashSet<>();
        for (TransitionDefinition transition : transitions) {
            String label = "transition '" + transition.action() + "' from '" + transition.from() + "'";
            if (transition.action() == null || !IDENTIFIER.matcher(transition.action()).matches()) {
                violations.add(label + " has an invalid action name");
            }
            if (!stateNames.contains(transition.from())) {
                violations.add(label + " starts at an unknown state");
            } else if (terminalStates.contains(transition.from())) {
                violations.add(label + " starts at a TERMINAL state");
            }
            if (!stateNames.contains(transition.to())) {
                violations.add(label + " targets unknown state '" + transition.to() + "'");
            }
            if (!transitionKeys.add(transition.from() + "\u0000" + transition.action())) {
                violations.add(label + " is duplicated");
            }
            statesWithOutgoing.add(transition.from());
        }

        for (StateDefinition state : states) {
            if (state.type() != null && state.type() != StateType.TERMINAL && !statesWithOutgoing.contains(state.name())) {
                violations.add("state '" + state.name() + "' is not TERMINAL and has no outgoing transition");
            }
        }

        if (initialStates.size() == 1) {
            Set<String> reachable = reachableFrom(initialStates.getFirst(), transitions);
            stateNames.stream()
                    .filter(name -> !reachable.contains(name))
                    .sorted()
                    .forEach(name -> violations.add("state '" + name + "' is unreachable from the INITIAL state"));
        }

        return violations;
    }

    private static List<String> namesOfType(List<StateDefinition> states, StateType type) {
        return states.stream()
                .filter(state -> state.type() == type)
                .map(StateDefinition::name)
                .toList();
    }

    private static Set<String> reachableFrom(String start, List<TransitionDefinition> transitions) {
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(start);
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (visited.add(current)) {
                transitions.stream()
                        .filter(transition -> current.equals(transition.from()))
                        .map(TransitionDefinition::to)
                        .forEach(pending::push);
            }
        }
        return visited;
    }

    void changeStatus(VersionStatus newStatus) {
        this.status = newStatus;
    }

    public String initialState() {
        return namesOfType(states, StateType.INITIAL).getFirst();
    }

    public Set<String> terminalStates() {
        return states.stream()
                .filter(state -> state.type() == StateType.TERMINAL)
                .map(StateDefinition::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<StateDefinition> state(String name) {
        return states.stream().filter(state -> state.name().equals(name)).findFirst();
    }

    public UUID id() {
        return id;
    }

    public int number() {
        return number;
    }

    public VersionStatus status() {
        return status;
    }

    public List<StateDefinition> states() {
        return states;
    }

    public List<TransitionDefinition> transitions() {
        return transitions;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

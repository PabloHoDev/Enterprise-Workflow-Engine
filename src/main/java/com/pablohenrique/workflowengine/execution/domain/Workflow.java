package com.pablohenrique.workflowengine.execution.domain;

import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluationResult;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate root do módulo Workflow Execution: a execução concreta de uma versão de Workflow Definition.
 *
 * <p>Toda mudança de estado passa por este aggregate, que só é alterado depois de todas as validações
 * (BR-022): uma operação recusada não deixa o Workflow parcialmente atualizado.
 */
public final class Workflow {

    private final UUID id;
    private final String definitionKey;
    private final UUID definitionVersionId;
    private final int definitionVersionNumber;
    private final Instant createdAt;
    private final List<HistoryEntry> history;
    private WorkflowStatus status;
    private String currentState;
    private Map<String, Object> variables;
    private Instant updatedAt;

    private Workflow(UUID id, String definitionKey, UUID definitionVersionId, int definitionVersionNumber,
                     WorkflowStatus status, String currentState, Map<String, Object> variables, Instant createdAt,
                     Instant updatedAt, List<HistoryEntry> history) {
        this.id = id;
        this.definitionKey = definitionKey;
        this.definitionVersionId = definitionVersionId;
        this.definitionVersionNumber = definitionVersionNumber;
        this.status = status;
        this.currentState = currentState;
        this.variables = new LinkedHashMap<>(variables);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.history = new ArrayList<>(history);
    }

    /** Cria a execução presa à versão informada e posicionada no seu State inicial (BR-010, BR-011). */
    public static Workflow create(VersionSnapshot definition, Map<String, Object> variables, Actor actor, Instant now) {
        Workflow workflow = new Workflow(UUID.randomUUID(), definition.definitionKey(), definition.versionId(),
                definition.versionNumber(), WorkflowStatus.CREATED, definition.initialState(),
                variables == null ? Map.of() : variables, now, now, List.of());
        workflow.record(HistoryEventType.CREATED, null, null, definition.initialState(), actor, now, null);
        return workflow;
    }

    public static Workflow restore(UUID id, String definitionKey, UUID definitionVersionId,
                                   int definitionVersionNumber, WorkflowStatus status, String currentState,
                                   Map<String, Object> variables, Instant createdAt, Instant updatedAt,
                                   List<HistoryEntry> history) {
        return new Workflow(id, definitionKey, definitionVersionId, definitionVersionNumber, status, currentState,
                variables, createdAt, updatedAt, history);
    }

    public void start(Actor actor, Instant now) {
        if (status != WorkflowStatus.CREATED) {
            throw new InvalidWorkflowStatusException(id, status, "be started");
        }
        status = WorkflowStatus.RUNNING;
        updatedAt = now;
        record(HistoryEventType.STARTED, null, currentState, currentState, actor, now, null);
    }

    /**
     * Executa a ação a partir do State atual (UC-009). As variáveis recebidas são combinadas às existentes
     * antes da avaliação das Rules e só são mantidas se a Transition for efetivada.
     */
    public void execute(String action, Map<String, Object> newVariables, VersionSnapshot definition, Actor actor,
                        RuleEvaluator ruleEvaluator, Instant now) {
        requireOwnDefinition(definition);
        if (status != WorkflowStatus.RUNNING) {
            throw new InvalidWorkflowStatusException(id, status, "execute actions");
        }
        TransitionSnapshot transition = definition.transition(currentState, action)
                .orElseThrow(() -> new TransitionNotAvailableException(id, currentState, action));
        if (transition.requiredRole() != null && !actor.hasRole(transition.requiredRole())) {
            throw new ActorNotAuthorizedException(actor.id(), action, transition.requiredRole());
        }
        Map<String, Object> mergedVariables = new LinkedHashMap<>(variables);
        if (newVariables != null) {
            mergedVariables.putAll(newVariables);
        }
        RuleEvaluationResult evaluation = ruleEvaluator.evaluate(transition.rules(), mergedVariables);
        if (!evaluation.satisfied()) {
            throw new RuleNotSatisfiedException(action, evaluation.unsatisfiedRules());
        }

        String previousState = currentState;
        variables = mergedVariables;
        currentState = transition.to();
        if (definition.isTerminal(currentState)) {
            status = WorkflowStatus.COMPLETED;
        }
        updatedAt = now;
        record(HistoryEventType.TRANSITIONED, action, previousState, currentState, actor, now, null);
    }

    /** Cancela a execução; permitido enquanto o Workflow não estiver encerrado (BR-035). */
    public void cancel(Actor actor, String reason, Instant now) {
        if (status.isTerminal()) {
            throw new InvalidWorkflowStatusException(id, status, "be cancelled");
        }
        status = WorkflowStatus.CANCELLED;
        updatedAt = now;
        record(HistoryEventType.CANCELLED, null, currentState, currentState, actor, now, reason);
    }

    /** Transitions que partem do State atual; vazio quando o Workflow não está em execução. */
    public List<TransitionSnapshot> availableTransitions(VersionSnapshot definition) {
        requireOwnDefinition(definition);
        return status == WorkflowStatus.RUNNING ? definition.transitionsFrom(currentState) : List.of();
    }

    private void requireOwnDefinition(VersionSnapshot definition) {
        // Invariante 16.2: a execução nunca troca de versão.
        if (!definitionVersionId.equals(definition.versionId())) {
            throw new IllegalArgumentException("Workflow " + id + " is bound to definition version "
                    + definitionVersionId + ", not " + definition.versionId());
        }
    }

    private void record(HistoryEventType type, String action, String fromState, String toState, Actor actor,
                        Instant now, String comment) {
        history.add(new HistoryEntry(history.size() + 1, type, action, fromState, toState, actor.id(), now, comment));
    }

    public UUID id() {
        return id;
    }

    public String definitionKey() {
        return definitionKey;
    }

    public UUID definitionVersionId() {
        return definitionVersionId;
    }

    public int definitionVersionNumber() {
        return definitionVersionNumber;
    }

    public WorkflowStatus status() {
        return status;
    }

    public String currentState() {
        return currentState;
    }

    public Map<String, Object> variables() {
        return Collections.unmodifiableMap(variables);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<HistoryEntry> history() {
        return Collections.unmodifiableList(history);
    }
}

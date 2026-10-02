package com.pablohenrique.workflowengine.support;

import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.definition.domain.StateDefinition;
import com.pablohenrique.workflowengine.definition.domain.StateType;
import com.pablohenrique.workflowengine.definition.domain.TransitionDefinition;
import com.pablohenrique.workflowengine.rules.contract.Rule;
import com.pablohenrique.workflowengine.rules.contract.RuleOperator;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Processo de referência usado nos testes: compras até 10.000 são aprovadas pelo gestor;
 * acima disso exigem validação financeira.
 */
public final class PurchaseApproval {

    public static final String REQUESTED = "REQUESTED";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String FINANCIAL_VALIDATION = "FINANCIAL_VALIDATION";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private PurchaseApproval() {
    }

    public static List<StateDefinition> states() {
        return List.of(
                new StateDefinition(REQUESTED, StateType.INITIAL),
                new StateDefinition(PENDING_APPROVAL, StateType.INTERMEDIATE),
                new StateDefinition(FINANCIAL_VALIDATION, StateType.INTERMEDIATE),
                new StateDefinition(APPROVED, StateType.TERMINAL),
                new StateDefinition(REJECTED, StateType.TERMINAL));
    }

    public static List<TransitionDefinition> transitions() {
        return List.of(
                new TransitionDefinition("submit", REQUESTED, PENDING_APPROVAL, null, List.of()),
                new TransitionDefinition("approve", PENDING_APPROVAL, APPROVED, "MANAGER",
                        List.of(new Rule("amount", RuleOperator.LESS_THAN_OR_EQUAL, "10000"))),
                new TransitionDefinition("send-to-finance", PENDING_APPROVAL, FINANCIAL_VALIDATION, "MANAGER",
                        List.of(new Rule("amount", RuleOperator.GREATER_THAN, "10000"))),
                new TransitionDefinition("reject", PENDING_APPROVAL, REJECTED, "MANAGER", List.of()),
                new TransitionDefinition("approve", FINANCIAL_VALIDATION, APPROVED, "FINANCE", List.of()),
                new TransitionDefinition("reject", FINANCIAL_VALIDATION, REJECTED, "FINANCE", List.of()));
    }

    public static VersionSnapshot snapshot() {
        List<TransitionSnapshot> transitions = transitions().stream()
                .map(transition -> new TransitionSnapshot(transition.action(), transition.from(), transition.to(),
                        transition.requiredRole(), transition.rules()))
                .toList();
        return new VersionSnapshot(UUID.randomUUID(), "purchase-approval", 1, REQUESTED,
                Set.of(APPROVED, REJECTED), transitions);
    }

    /** Corpo JSON da estrutura (States e Transitions) aceito pela API. */
    public static String structureJson() {
        return """
                "states": [
                  {"name": "REQUESTED", "type": "INITIAL"},
                  {"name": "PENDING_APPROVAL", "type": "INTERMEDIATE"},
                  {"name": "FINANCIAL_VALIDATION", "type": "INTERMEDIATE"},
                  {"name": "APPROVED", "type": "TERMINAL"},
                  {"name": "REJECTED", "type": "TERMINAL"}
                ],
                "transitions": [
                  {"action": "submit", "from": "REQUESTED", "to": "PENDING_APPROVAL"},
                  {"action": "approve", "from": "PENDING_APPROVAL", "to": "APPROVED", "requiredRole": "MANAGER",
                   "rules": [{"field": "amount", "operator": "LESS_THAN_OR_EQUAL", "value": "10000"}]},
                  {"action": "send-to-finance", "from": "PENDING_APPROVAL", "to": "FINANCIAL_VALIDATION",
                   "requiredRole": "MANAGER",
                   "rules": [{"field": "amount", "operator": "GREATER_THAN", "value": "10000"}]},
                  {"action": "reject", "from": "PENDING_APPROVAL", "to": "REJECTED", "requiredRole": "MANAGER"},
                  {"action": "approve", "from": "FINANCIAL_VALIDATION", "to": "APPROVED", "requiredRole": "FINANCE"},
                  {"action": "reject", "from": "FINANCIAL_VALIDATION", "to": "REJECTED", "requiredRole": "FINANCE"}
                ]
                """;
    }
}

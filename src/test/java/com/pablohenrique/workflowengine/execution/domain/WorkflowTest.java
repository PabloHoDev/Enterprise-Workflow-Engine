package com.pablohenrique.workflowengine.execution.domain;

import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluator;
import com.pablohenrique.workflowengine.rules.domain.DeterministicRuleEvaluator;
import com.pablohenrique.workflowengine.support.PurchaseApproval;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class WorkflowTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Actor REQUESTER = new Actor("ana", Set.of("USER"));
    private static final Actor MANAGER = new Actor("manager-123", Set.of("USER", "MANAGER"));
    private static final Actor FINANCE = new Actor("finance-7", Set.of("USER", "FINANCE"));

    private final VersionSnapshot definition = PurchaseApproval.snapshot();
    private final RuleEvaluator rules = new DeterministicRuleEvaluator();

    private Workflow newWorkflow(int amount) {
        return Workflow.create(definition, Map.of("amount", amount), REQUESTER, NOW);
    }

    private Workflow pendingApproval(int amount) {
        Workflow workflow = newWorkflow(amount);
        workflow.start(REQUESTER, NOW);
        workflow.execute("submit", null, definition, REQUESTER, rules, NOW);
        return workflow;
    }

    @Test
    void isCreatedAtTheInitialStateBoundToTheDefinitionVersion() {
        Workflow workflow = newWorkflow(500);

        assertThat(workflow.status()).isEqualTo(WorkflowStatus.CREATED);
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.REQUESTED);
        assertThat(workflow.definitionKey()).isEqualTo("purchase-approval");
        assertThat(workflow.definitionVersionId()).isEqualTo(definition.versionId());
        assertThat(workflow.definitionVersionNumber()).isEqualTo(1);
        assertThat(workflow.variables()).containsEntry("amount", 500);
        assertThat(workflow.history()).singleElement().satisfies(entry -> {
            assertThat(entry.sequence()).isEqualTo(1);
            assertThat(entry.type()).isEqualTo(HistoryEventType.CREATED);
            assertThat(entry.fromState()).isNull();
            assertThat(entry.toState()).isEqualTo(PurchaseApproval.REQUESTED);
            assertThat(entry.actorId()).isEqualTo("ana");
            assertThat(entry.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void createdWithoutVariablesHasAnEmptyContext() {
        Workflow workflow = Workflow.create(definition, null, REQUESTER, NOW);

        assertThat(workflow.variables()).isEmpty();
    }

    @Test
    void doesNotAcceptActionsBeforeBeingStarted() {
        Workflow workflow = newWorkflow(500);

        assertThatThrownBy(() -> workflow.execute("submit", null, definition, REQUESTER, rules, NOW))
                .isInstanceOf(InvalidWorkflowStatusException.class)
                .hasMessageContaining("because it is CREATED");
        assertThat(workflow.availableTransitions(definition)).isEmpty();
    }

    @Test
    void startMovesToRunningOnlyOnce() {
        Workflow workflow = newWorkflow(500);
        Instant startedAt = NOW.plusSeconds(5);

        workflow.start(REQUESTER, startedAt);

        assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.REQUESTED);
        assertThat(workflow.updatedAt()).isEqualTo(startedAt);
        assertThat(workflow.history()).extracting(HistoryEntry::type)
                .containsExactly(HistoryEventType.CREATED, HistoryEventType.STARTED);
        assertThatThrownBy(() -> workflow.start(REQUESTER, startedAt))
                .isInstanceOf(InvalidWorkflowStatusException.class);
    }

    @Test
    void executesAValidTransitionAndRecordsIt() {
        Workflow workflow = newWorkflow(500);
        workflow.start(REQUESTER, NOW);
        Instant submittedAt = NOW.plusSeconds(30);

        workflow.execute("submit", null, definition, REQUESTER, rules, submittedAt);

        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.PENDING_APPROVAL);
        assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(workflow.updatedAt()).isEqualTo(submittedAt);
        HistoryEntry last = workflow.history().getLast();
        assertThat(last.sequence()).isEqualTo(3);
        assertThat(last.type()).isEqualTo(HistoryEventType.TRANSITIONED);
        assertThat(last.action()).isEqualTo("submit");
        assertThat(last.fromState()).isEqualTo(PurchaseApproval.REQUESTED);
        assertThat(last.toState()).isEqualTo(PurchaseApproval.PENDING_APPROVAL);
        assertThat(last.occurredAt()).isEqualTo(submittedAt);
    }

    @Test
    void exposesOnlyTheTransitionsOfTheCurrentState() {
        Workflow workflow = pendingApproval(500);

        assertThat(workflow.availableTransitions(definition)).extracting(TransitionSnapshot::action)
                .containsExactly("approve", "send-to-finance", "reject");
    }

    @Test
    void rejectsActionsNotDefinedForTheCurrentState() {
        Workflow workflow = newWorkflow(500);
        workflow.start(REQUESTER, NOW);

        assertThatThrownBy(() -> workflow.execute("approve", null, definition, MANAGER, rules, NOW))
                .isInstanceOf(TransitionNotAvailableException.class)
                .hasMessageContaining("'approve' is not available")
                .hasMessageContaining("'REQUESTED'");
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.REQUESTED);
    }

    @Test
    void rejectsActorsWithoutTheRequiredRole() {
        Workflow workflow = pendingApproval(500);
        int historySize = workflow.history().size();

        assertThatThrownBy(() -> workflow.execute("approve", null, definition, REQUESTER, rules, NOW))
                .isInstanceOf(ActorNotAuthorizedException.class)
                .hasMessageContaining("role 'MANAGER' is required");
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.PENDING_APPROVAL);
        assertThat(workflow.history()).hasSize(historySize);
    }

    @Test
    void rejectsTransitionsWhoseRulesAreNotSatisfiedWithoutChangingTheWorkflow() {
        Workflow workflow = pendingApproval(15000);
        int historySize = workflow.history().size();

        RuleNotSatisfiedException exception = catchThrowableOfType(RuleNotSatisfiedException.class,
                () -> workflow.execute("approve", Map.of("note", "rush"), definition, MANAGER, rules, NOW));

        assertThat(exception.unsatisfiedRules()).containsExactly("amount LESS_THAN_OR_EQUAL 10000");
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.PENDING_APPROVAL);
        assertThat(workflow.history()).hasSize(historySize);
        assertThat(workflow.variables()).doesNotContainKey("note");
    }

    @Test
    void variablesSentWithTheActionTakePartInRuleEvaluationAndAreKept() {
        Workflow workflow = pendingApproval(15000);

        workflow.execute("approve", Map.of("amount", 8000, "note", "renegotiated"), definition, MANAGER, rules, NOW);

        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.APPROVED);
        assertThat(workflow.variables()).containsEntry("amount", 8000).containsEntry("note", "renegotiated");
    }

    @Test
    void reachingATerminalStateCompletesTheWorkflowAndBlocksFurtherOperations() {
        Workflow workflow = pendingApproval(15000);
        workflow.execute("send-to-finance", null, definition, MANAGER, rules, NOW);

        workflow.execute("approve", null, definition, FINANCE, rules, NOW);

        assertThat(workflow.status()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.APPROVED);
        assertThat(workflow.history().getLast().toState()).isEqualTo(PurchaseApproval.APPROVED);
        assertThat(workflow.availableTransitions(definition)).isEmpty();
        assertThatThrownBy(() -> workflow.execute("approve", null, definition, FINANCE, rules, NOW))
                .isInstanceOf(InvalidWorkflowStatusException.class)
                .hasMessageContaining("because it is COMPLETED");
        assertThatThrownBy(() -> workflow.cancel(FINANCE, null, NOW))
                .isInstanceOf(InvalidWorkflowStatusException.class);
    }

    @Test
    void canBeCancelledWhileNotFinishedKeepingTheStateWhereItStopped() {
        Workflow workflow = pendingApproval(500);
        Instant cancelledAt = NOW.plusSeconds(90);

        workflow.cancel(MANAGER, "duplicated request", cancelledAt);

        assertThat(workflow.status()).isEqualTo(WorkflowStatus.CANCELLED);
        assertThat(workflow.currentState()).isEqualTo(PurchaseApproval.PENDING_APPROVAL);
        HistoryEntry last = workflow.history().getLast();
        assertThat(last.type()).isEqualTo(HistoryEventType.CANCELLED);
        assertThat(last.actorId()).isEqualTo("manager-123");
        assertThat(last.comment()).isEqualTo("duplicated request");
        assertThat(last.occurredAt()).isEqualTo(cancelledAt);
        assertThatThrownBy(() -> workflow.cancel(MANAGER, null, cancelledAt))
                .isInstanceOf(InvalidWorkflowStatusException.class)
                .hasMessageContaining("because it is CANCELLED");
        assertThatThrownBy(() -> workflow.start(REQUESTER, cancelledAt))
                .isInstanceOf(InvalidWorkflowStatusException.class);
    }

    @Test
    void canBeCancelledBeforeBeingStarted() {
        Workflow workflow = newWorkflow(500);

        workflow.cancel(REQUESTER, null, NOW);

        assertThat(workflow.status()).isEqualTo(WorkflowStatus.CANCELLED);
    }

    @Test
    void neverRunsAgainstADifferentDefinitionVersion() {
        Workflow workflow = newWorkflow(500);
        workflow.start(REQUESTER, NOW);
        VersionSnapshot otherVersion = PurchaseApproval.snapshot();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> workflow.execute("submit", null, otherVersion, REQUESTER, rules, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> workflow.availableTransitions(otherVersion));
    }

    @Test
    void historySequenceIsContiguous() {
        Workflow workflow = pendingApproval(15000);
        workflow.execute("send-to-finance", null, definition, MANAGER, rules, NOW);
        workflow.execute("reject", null, definition, FINANCE, rules, NOW);

        assertThat(workflow.history()).extracting(HistoryEntry::sequence).containsExactly(1, 2, 3, 4, 5);
        assertThat(workflow.history()).extracting(HistoryEntry::toState).containsExactly(
                PurchaseApproval.REQUESTED, PurchaseApproval.REQUESTED, PurchaseApproval.PENDING_APPROVAL,
                PurchaseApproval.FINANCIAL_VALIDATION, PurchaseApproval.REJECTED);
    }

    @Test
    void actorRequiresAnIdentifier() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Actor(" ", Set.of()));
        assertThat(new Actor("system", null).roles()).isEmpty();
    }
}

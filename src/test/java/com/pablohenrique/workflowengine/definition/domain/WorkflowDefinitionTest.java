package com.pablohenrique.workflowengine.definition.domain;

import com.pablohenrique.workflowengine.support.PurchaseApproval;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class WorkflowDefinitionTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);

    private static WorkflowDefinition purchaseApproval() {
        return WorkflowDefinition.create("purchase-approval", "Purchase Approval", "Aprovação de compras",
                PurchaseApproval.states(), PurchaseApproval.transitions(), NOW);
    }

    private static List<String> violationsOf(List<StateDefinition> states, List<TransitionDefinition> transitions) {
        InvalidDefinitionException exception = catchThrowableOfType(InvalidDefinitionException.class,
                () -> WorkflowDefinition.create("some-process", "Some process", null, states, transitions, NOW));
        assertThat(exception).as("definition should have been rejected").isNotNull();
        return exception.violations();
    }

    @Nested
    class Creation {

        @Test
        void startsWithASingleDraftVersion() {
            WorkflowDefinition definition = purchaseApproval();

            assertThat(definition.id()).isNotNull();
            assertThat(definition.key()).isEqualTo("purchase-approval");
            assertThat(definition.createdAt()).isEqualTo(NOW);
            assertThat(definition.activeVersion()).isEmpty();
            assertThat(definition.versions()).singleElement().satisfies(version -> {
                assertThat(version.number()).isEqualTo(1);
                assertThat(version.status()).isEqualTo(VersionStatus.DRAFT);
                assertThat(version.initialState()).isEqualTo(PurchaseApproval.REQUESTED);
                assertThat(version.terminalStates())
                        .containsExactlyInAnyOrder(PurchaseApproval.APPROVED, PurchaseApproval.REJECTED);
                assertThat(version.state(PurchaseApproval.APPROVED)).isPresent();
                assertThat(version.state("UNKNOWN")).isEmpty();
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Purchase", "purchase_approval", "-purchase", "purchase-", "purchase--approval"})
        void rejectsKeysThatAreNotKebabCase(String key) {
            assertThatThrownBy(() -> WorkflowDefinition.create(key, "Name", null, PurchaseApproval.states(),
                    PurchaseApproval.transitions(), NOW))
                    .isInstanceOf(InvalidDefinitionException.class)
                    .hasMessageContaining("key must be lowercase kebab-case");
        }

        @Test
        void rejectsMissingNameAndOversizedDescription() {
            InvalidDefinitionException exception = catchThrowableOfType(InvalidDefinitionException.class,
                    () -> WorkflowDefinition.create("valid-key", " ", "x".repeat(1001), PurchaseApproval.states(),
                            PurchaseApproval.transitions(), NOW));

            assertThat(exception.violations()).hasSize(2);
        }
    }

    @Nested
    class StructuralValidation {

        @Test
        void requiresStates() {
            assertThat(violationsOf(List.of(), List.of())).containsExactly("at least one state is required");
            assertThat(violationsOf(null, null)).containsExactly("at least one state is required");
        }

        @Test
        void requiresExactlyOneInitialAndAtLeastOneTerminalState() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("A", StateType.INITIAL),
                    new StateDefinition("B", StateType.INITIAL));
            List<TransitionDefinition> transitions = List.of(
                    new TransitionDefinition("go", "A", "B", null, List.of()),
                    new TransitionDefinition("back", "B", "A", null, List.of()));

            assertThat(violationsOf(states, transitions)).contains(
                    "exactly one INITIAL state is required, found 2",
                    "at least one TERMINAL state is required");
        }

        @Test
        void rejectsDuplicatedInvalidAndUntypedStates() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("START", StateType.INITIAL),
                    new StateDefinition("START", StateType.TERMINAL),
                    new StateDefinition("not valid", StateType.TERMINAL),
                    new StateDefinition("UNTYPED", null));

            assertThat(violationsOf(states, List.of())).contains(
                    "state 'START' is duplicated",
                    "state name 'not valid' is invalid",
                    "state 'UNTYPED' has no type");
        }

        @Test
        void rejectsTransitionsThatReferenceUnknownStatesOrLeaveTerminalOnes() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("START", StateType.INITIAL),
                    new StateDefinition("END", StateType.TERMINAL));
            List<TransitionDefinition> transitions = List.of(
                    new TransitionDefinition("finish", "START", "END", null, List.of()),
                    new TransitionDefinition("reopen", "END", "START", null, List.of()),
                    new TransitionDefinition("jump", "NOWHERE", "END", null, List.of()),
                    new TransitionDefinition("lose", "START", "VOID", null, List.of()),
                    new TransitionDefinition("bad action", "START", "END", null, List.of()));

            assertThat(violationsOf(states, transitions)).contains(
                    "transition 'reopen' from 'END' starts at a TERMINAL state",
                    "transition 'jump' from 'NOWHERE' starts at an unknown state",
                    "transition 'lose' from 'START' targets unknown state 'VOID'",
                    "transition 'bad action' from 'START' has an invalid action name");
        }

        @Test
        void rejectsAmbiguousActionsFromTheSameState() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("START", StateType.INITIAL),
                    new StateDefinition("DONE", StateType.TERMINAL),
                    new StateDefinition("FAILED", StateType.TERMINAL));
            List<TransitionDefinition> transitions = List.of(
                    new TransitionDefinition("finish", "START", "DONE", null, List.of()),
                    new TransitionDefinition("finish", "START", "FAILED", null, List.of()));

            assertThat(violationsOf(states, transitions))
                    .containsExactly("transition 'finish' from 'START' is duplicated");
        }

        @Test
        void rejectsDeadEndsAndUnreachableStates() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("START", StateType.INITIAL),
                    new StateDefinition("STUCK", StateType.INTERMEDIATE),
                    new StateDefinition("ISLAND", StateType.INTERMEDIATE),
                    new StateDefinition("END", StateType.TERMINAL));
            List<TransitionDefinition> transitions = List.of(
                    new TransitionDefinition("finish", "START", "END", null, List.of()),
                    new TransitionDefinition("wait", "START", "STUCK", null, List.of()),
                    new TransitionDefinition("leave", "ISLAND", "END", null, List.of()));

            assertThat(violationsOf(states, transitions)).containsExactlyInAnyOrder(
                    "state 'STUCK' is not TERMINAL and has no outgoing transition",
                    "state 'ISLAND' is unreachable from the INITIAL state");
        }

        @Test
        void acceptsLoopsBetweenNonTerminalStates() {
            List<StateDefinition> states = List.of(
                    new StateDefinition("DRAFT", StateType.INITIAL),
                    new StateDefinition("REVIEW", StateType.INTERMEDIATE),
                    new StateDefinition("PUBLISHED", StateType.TERMINAL));
            List<TransitionDefinition> transitions = List.of(
                    new TransitionDefinition("submit", "DRAFT", "REVIEW", null, List.of()),
                    new TransitionDefinition("request-changes", "REVIEW", "DRAFT", null, List.of()),
                    new TransitionDefinition("publish", "REVIEW", "PUBLISHED", null, List.of()));

            WorkflowDefinition definition =
                    WorkflowDefinition.create("publishing", "Publishing", null, states, transitions, NOW);

            assertThat(definition.versions()).hasSize(1);
        }
    }

    @Nested
    class Versioning {

        @Test
        void newVersionGetsTheNextNumberAndLeavesPreviousOnesUntouched() {
            WorkflowDefinition definition = purchaseApproval();
            DefinitionVersion first = definition.requireVersion(1);

            DefinitionVersion second =
                    definition.addVersion(PurchaseApproval.states(), PurchaseApproval.transitions(), LATER);

            assertThat(second.number()).isEqualTo(2);
            assertThat(second.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(second.id()).isNotEqualTo(first.id());
            assertThat(definition.versions()).containsExactly(first, second);
            assertThat(first.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(definition.updatedAt()).isEqualTo(LATER);
        }

        @Test
        void invalidNewVersionIsRejectedAndNotAdded() {
            WorkflowDefinition definition = purchaseApproval();

            assertThatThrownBy(() -> definition.addVersion(List.of(), List.of(), LATER))
                    .isInstanceOf(InvalidDefinitionException.class);
            assertThat(definition.versions()).hasSize(1);
        }

        @Test
        void activatingAVersionRetiresThePreviouslyActiveOne() {
            WorkflowDefinition definition = purchaseApproval();
            definition.addVersion(PurchaseApproval.states(), PurchaseApproval.transitions(), NOW);
            definition.activate(1, NOW);

            definition.activate(2, LATER);

            assertThat(definition.requireVersion(1).status()).isEqualTo(VersionStatus.INACTIVE);
            assertThat(definition.requireVersion(2).status()).isEqualTo(VersionStatus.ACTIVE);
            assertThat(definition.activeVersion()).map(DefinitionVersion::number).contains(2);
        }

        @Test
        void inactiveVersionCanBeActivatedAgain() {
            WorkflowDefinition definition = purchaseApproval();
            definition.activate(1, NOW);
            definition.deactivate(1, NOW);

            definition.activate(1, LATER);

            assertThat(definition.activeVersion()).map(DefinitionVersion::number).contains(1);
        }

        @Test
        void deactivationLeavesNoActiveVersion() {
            WorkflowDefinition definition = purchaseApproval();
            definition.activate(1, NOW);

            definition.deactivate(1, LATER);

            assertThat(definition.activeVersion()).isEmpty();
            assertThat(definition.requireVersion(1).status()).isEqualTo(VersionStatus.INACTIVE);
        }

        @Test
        void rejectsStatusChangesThatDoNotApply() {
            WorkflowDefinition definition = purchaseApproval();

            assertThatThrownBy(() -> definition.deactivate(1, NOW))
                    .isInstanceOf(InvalidVersionStatusException.class)
                    .hasMessageContaining("cannot be deactivated because it is DRAFT");

            definition.activate(1, NOW);
            assertThatThrownBy(() -> definition.activate(1, NOW))
                    .isInstanceOf(InvalidVersionStatusException.class)
                    .hasMessageContaining("cannot be activated because it is ACTIVE");
        }

        @Test
        void unknownVersionIsReported() {
            WorkflowDefinition definition = purchaseApproval();

            assertThatThrownBy(() -> definition.activate(7, NOW))
                    .isInstanceOf(DefinitionVersionNotFoundException.class)
                    .hasMessageContaining("has no version 7");
        }
    }
}

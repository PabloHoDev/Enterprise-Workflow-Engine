package com.pablohenrique.workflowengine.definition.interfaces.rest;

import com.pablohenrique.workflowengine.definition.application.DefinitionSummary;
import com.pablohenrique.workflowengine.definition.domain.DefinitionVersion;
import com.pablohenrique.workflowengine.definition.domain.StateType;
import com.pablohenrique.workflowengine.definition.domain.VersionStatus;
import com.pablohenrique.workflowengine.definition.domain.WorkflowDefinition;
import com.pablohenrique.workflowengine.rules.contract.RuleOperator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class DefinitionResponses {

    private DefinitionResponses() {
    }

    record DefinitionSummaryResponse(UUID id, String key, String name, String description, Instant createdAt,
                                     Instant updatedAt) {

        static DefinitionSummaryResponse from(DefinitionSummary summary) {
            return new DefinitionSummaryResponse(summary.id(), summary.key(), summary.name(), summary.description(),
                    summary.createdAt(), summary.updatedAt());
        }
    }

    record DefinitionResponse(UUID id, String key, String name, String description, Instant createdAt,
                              Instant updatedAt, Integer activeVersion, List<VersionSummaryResponse> versions) {

        static DefinitionResponse from(WorkflowDefinition definition) {
            return new DefinitionResponse(definition.id(), definition.key(), definition.name(),
                    definition.description(), definition.createdAt(), definition.updatedAt(),
                    definition.activeVersion().map(DefinitionVersion::number).orElse(null),
                    definition.versions().stream()
                            .map(version -> new VersionSummaryResponse(version.number(), version.status(),
                                    version.createdAt()))
                            .toList());
        }
    }

    record VersionSummaryResponse(int number, VersionStatus status, Instant createdAt) {
    }

    record VersionResponse(UUID id, String definitionKey, int number, VersionStatus status, Instant createdAt,
                           List<StateResponse> states, List<TransitionResponse> transitions) {

        static VersionResponse from(String definitionKey, DefinitionVersion version) {
            return new VersionResponse(version.id(), definitionKey, version.number(), version.status(),
                    version.createdAt(),
                    version.states().stream()
                            .map(state -> new StateResponse(state.name(), state.type()))
                            .toList(),
                    version.transitions().stream()
                            .map(transition -> new TransitionResponse(transition.action(), transition.from(),
                                    transition.to(), transition.requiredRole(),
                                    transition.rules().stream()
                                            .map(rule -> new RuleResponse(rule.field(), rule.operator(),
                                                    rule.expectedValue()))
                                            .toList()))
                            .toList());
        }
    }

    record StateResponse(String name, StateType type) {
    }

    record TransitionResponse(String action, String from, String to, String requiredRole, List<RuleResponse> rules) {
    }

    record RuleResponse(String field, RuleOperator operator, String value) {
    }
}

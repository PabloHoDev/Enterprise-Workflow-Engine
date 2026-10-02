package com.pablohenrique.workflowengine.definition.interfaces.rest;

import com.pablohenrique.workflowengine.definition.domain.InvalidDefinitionException;
import com.pablohenrique.workflowengine.definition.domain.StateDefinition;
import com.pablohenrique.workflowengine.definition.domain.StateType;
import com.pablohenrique.workflowengine.definition.domain.TransitionDefinition;
import com.pablohenrique.workflowengine.rules.contract.Rule;
import com.pablohenrique.workflowengine.rules.contract.RuleOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

final class DefinitionRequests {

    private DefinitionRequests() {
    }

    record CreateDefinitionRequest(
            @NotBlank @Size(max = 64) String key,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @NotEmpty List<@NotNull @Valid StateRequest> states,
            @NotNull List<@NotNull @Valid TransitionRequest> transitions) {
    }

    record CreateVersionRequest(
            @NotEmpty List<@NotNull @Valid StateRequest> states,
            @NotNull List<@NotNull @Valid TransitionRequest> transitions) {
    }

    record StateRequest(@NotBlank @Size(max = 64) String name, @NotNull StateType type) {

        StateDefinition toDomain() {
            return new StateDefinition(name, type);
        }
    }

    record TransitionRequest(
            @NotBlank @Size(max = 64) String action,
            @NotBlank @Size(max = 64) String from,
            @NotBlank @Size(max = 64) String to,
            @Size(max = 64) String requiredRole,
            List<@NotNull @Valid RuleRequest> rules) {

        TransitionDefinition toDomain() {
            List<Rule> domainRules = rules == null ? List.of() : rules.stream().map(RuleRequest::toDomain).toList();
            return new TransitionDefinition(action, from, to, requiredRole, domainRules);
        }
    }

    record RuleRequest(
            @NotBlank @Size(max = 128) String field,
            @NotNull RuleOperator operator,
            @Size(max = 255) String value) {

        Rule toDomain() {
            if (operator.requiresExpectedValue() && value == null) {
                throw new InvalidDefinitionException(
                        List.of("rule on field '" + field + "' with operator " + operator + " requires a value"));
            }
            return new Rule(field, operator, value);
        }
    }

    static List<StateDefinition> toStates(List<StateRequest> states) {
        return states.stream().map(StateRequest::toDomain).toList();
    }

    static List<TransitionDefinition> toTransitions(List<TransitionRequest> transitions) {
        return transitions.stream().map(TransitionRequest::toDomain).toList();
    }
}

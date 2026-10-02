package com.pablohenrique.workflowengine.execution.domain;

import com.pablohenrique.workflowengine.rules.contract.Rule;

import java.util.List;

/** Ao menos uma Rule obrigatória da Transition não foi satisfeita (BR-021, BR-025). */
public class RuleNotSatisfiedException extends WorkflowException {

    private final List<String> unsatisfiedRules;

    public RuleNotSatisfiedException(String action, List<Rule> unsatisfiedRules) {
        super("Action '" + action + "' was rejected because its rules are not satisfied");
        this.unsatisfiedRules = unsatisfiedRules.stream().map(Rule::describe).toList();
    }

    public List<String> unsatisfiedRules() {
        return unsatisfiedRules;
    }
}

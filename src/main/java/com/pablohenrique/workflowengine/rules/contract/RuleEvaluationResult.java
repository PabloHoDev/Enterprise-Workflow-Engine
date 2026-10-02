package com.pablohenrique.workflowengine.rules.contract;

import java.util.List;

public record RuleEvaluationResult(List<Rule> unsatisfiedRules) {

    public RuleEvaluationResult {
        unsatisfiedRules = List.copyOf(unsatisfiedRules);
    }

    public boolean satisfied() {
        return unsatisfiedRules.isEmpty();
    }
}

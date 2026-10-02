package com.pablohenrique.workflowengine.rules.contract;

public enum RuleOperator {

    EQUALS(true),
    NOT_EQUALS(true),
    GREATER_THAN(true),
    GREATER_THAN_OR_EQUAL(true),
    LESS_THAN(true),
    LESS_THAN_OR_EQUAL(true),
    EXISTS(false);

    private final boolean requiresExpectedValue;

    RuleOperator(boolean requiresExpectedValue) {
        this.requiresExpectedValue = requiresExpectedValue;
    }

    public boolean requiresExpectedValue() {
        return requiresExpectedValue;
    }
}

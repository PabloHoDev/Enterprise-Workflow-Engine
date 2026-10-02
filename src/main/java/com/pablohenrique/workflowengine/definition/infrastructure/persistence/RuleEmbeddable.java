package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import com.pablohenrique.workflowengine.rules.contract.RuleOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
class RuleEmbeddable {

    @Column(name = "field_name", nullable = false)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_operator", nullable = false)
    private RuleOperator operator;

    @Column(name = "expected_value")
    private String expectedValue;

    protected RuleEmbeddable() {
    }

    RuleEmbeddable(String field, RuleOperator operator, String expectedValue) {
        this.field = field;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    String getField() {
        return field;
    }

    RuleOperator getOperator() {
        return operator;
    }

    String getExpectedValue() {
        return expectedValue;
    }
}

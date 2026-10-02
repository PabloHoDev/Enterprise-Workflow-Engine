package com.pablohenrique.workflowengine.rules.contract;

import java.util.Objects;

/**
 * Condição declarativa avaliada contra as variáveis de um Workflow.
 *
 * @param field         caminho da variável; níveis aninhados são separados por ponto ({@code purchase.amount})
 * @param operator      operador de comparação
 * @param expectedValue valor de referência; ignorado por operadores que não o exigem
 */
public record Rule(String field, RuleOperator operator, String expectedValue) {

    public Rule {
        Objects.requireNonNull(operator, "operator");
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Rule field must not be blank");
        }
        if (operator.requiresExpectedValue() && expectedValue == null) {
            throw new IllegalArgumentException("Rule operator " + operator + " requires an expected value");
        }
    }

    public String describe() {
        return operator.requiresExpectedValue()
                ? field + " " + operator + " " + expectedValue
                : field + " " + operator;
    }
}

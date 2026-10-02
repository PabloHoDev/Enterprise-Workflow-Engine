package com.pablohenrique.workflowengine.rules.domain;

import com.pablohenrique.workflowengine.rules.contract.Rule;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluationResult;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Avaliação determinística (BR-024): o resultado depende apenas das regras e das variáveis recebidas.
 */
public final class DeterministicRuleEvaluator implements RuleEvaluator {

    @Override
    public RuleEvaluationResult evaluate(List<Rule> rules, Map<String, Object> variables) {
        List<Rule> unsatisfied = rules.stream()
                .filter(rule -> !isSatisfied(rule, variables))
                .toList();
        return new RuleEvaluationResult(unsatisfied);
    }

    private boolean isSatisfied(Rule rule, Map<String, Object> variables) {
        Object actual = resolve(rule.field(), variables);
        if (actual == null) {
            // Variável ausente nunca satisfaz uma regra, inclusive NOT_EQUALS: a ausência não é evidência.
            return false;
        }
        return switch (rule.operator()) {
            case EXISTS -> true;
            case EQUALS -> areEqual(actual, rule.expectedValue());
            case NOT_EQUALS -> !areEqual(actual, rule.expectedValue());
            case GREATER_THAN -> compare(actual, rule.expectedValue()).map(c -> c > 0).orElse(false);
            case GREATER_THAN_OR_EQUAL -> compare(actual, rule.expectedValue()).map(c -> c >= 0).orElse(false);
            case LESS_THAN -> compare(actual, rule.expectedValue()).map(c -> c < 0).orElse(false);
            case LESS_THAN_OR_EQUAL -> compare(actual, rule.expectedValue()).map(c -> c <= 0).orElse(false);
        };
    }

    private Object resolve(String field, Map<String, Object> variables) {
        Object current = variables;
        for (String segment : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private boolean areEqual(Object actual, String expected) {
        return compare(actual, expected)
                .map(c -> c == 0)
                .orElseGet(() -> actual.toString().equals(expected));
    }

    /** Comparação numérica; vazio quando algum dos lados não é um número. */
    private Optional<Integer> compare(Object actual, String expected) {
        Optional<BigDecimal> left = toNumber(actual);
        Optional<BigDecimal> right = toNumber(expected);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(left.get().compareTo(right.get()));
    }

    private Optional<BigDecimal> toNumber(Object value) {
        if (value instanceof Boolean) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.toString().trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

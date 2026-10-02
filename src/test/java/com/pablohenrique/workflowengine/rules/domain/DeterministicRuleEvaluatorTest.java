package com.pablohenrique.workflowengine.rules.domain;

import com.pablohenrique.workflowengine.rules.contract.Rule;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluationResult;
import com.pablohenrique.workflowengine.rules.contract.RuleOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DeterministicRuleEvaluatorTest {

    private final DeterministicRuleEvaluator evaluator = new DeterministicRuleEvaluator();

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @CsvSource({
            "15000, GREATER_THAN, 10000, true",
            "10000, GREATER_THAN, 10000, false",
            "10000, GREATER_THAN_OR_EQUAL, 10000, true",
            "9999.99, LESS_THAN, 10000, true",
            "10000, LESS_THAN, 10000, false",
            "10000, LESS_THAN_OR_EQUAL, 10000, true",
            "10000.00, EQUALS, 10000, true",
            "10001, EQUALS, 10000, false",
            "10001, NOT_EQUALS, 10000, true",
            "9, GREATER_THAN, 10, false"
    })
    void comparesNumbersByValue(String actual, RuleOperator operator, String expected, boolean satisfied) {
        Rule rule = new Rule("amount", operator, expected);

        RuleEvaluationResult result = evaluator.evaluate(List.of(rule), Map.of("amount", actual));

        assertThat(result.satisfied()).isEqualTo(satisfied);
    }

    @Test
    void comparesNumericVariableTypesAgainstTextualExpectedValue() {
        Rule rule = new Rule("amount", RuleOperator.GREATER_THAN, "10000");

        assertThat(evaluator.evaluate(List.of(rule), Map.of("amount", 15000)).satisfied()).isTrue();
        assertThat(evaluator.evaluate(List.of(rule), Map.of("amount", 15000.5)).satisfied()).isTrue();
    }

    @Test
    void comparesTextAndBooleansByEquality() {
        Map<String, Object> variables = Map.of("department", "finance", "urgent", true);

        assertThat(evaluator.evaluate(List.of(new Rule("department", RuleOperator.EQUALS, "finance")), variables)
                .satisfied()).isTrue();
        assertThat(evaluator.evaluate(List.of(new Rule("department", RuleOperator.NOT_EQUALS, "sales")), variables)
                .satisfied()).isTrue();
        assertThat(evaluator.evaluate(List.of(new Rule("urgent", RuleOperator.EQUALS, "true")), variables)
                .satisfied()).isTrue();
    }

    @Test
    void orderingOperatorsAreNeverSatisfiedByNonNumericValues() {
        Rule rule = new Rule("department", RuleOperator.GREATER_THAN, "10");

        assertThat(evaluator.evaluate(List.of(rule), Map.of("department", "finance")).satisfied()).isFalse();
        assertThat(evaluator.evaluate(List.of(rule), Map.of("department", true)).satisfied()).isFalse();
    }

    @Test
    void missingVariableNeverSatisfiesARule() {
        Map<String, Object> variables = Map.of("other", 1);

        for (RuleOperator operator : RuleOperator.values()) {
            Rule rule = new Rule("amount", operator, "10");
            assertThat(evaluator.evaluate(List.of(rule), variables).satisfied())
                    .as("operator %s", operator)
                    .isFalse();
        }
    }

    @Test
    void existsIsSatisfiedByAnyPresentValue() {
        Rule rule = new Rule("justification", RuleOperator.EXISTS, null);

        assertThat(evaluator.evaluate(List.of(rule), Map.of("justification", "")).satisfied()).isTrue();
    }

    @Test
    void resolvesNestedVariablesByDottedPath() {
        Rule rule = new Rule("purchase.amount", RuleOperator.GREATER_THAN, "100");
        Map<String, Object> variables = Map.of("purchase", Map.of("amount", 250));

        assertThat(evaluator.evaluate(List.of(rule), variables).satisfied()).isTrue();
        assertThat(evaluator.evaluate(List.of(new Rule("purchase.amount.cents", RuleOperator.EXISTS, null)), variables)
                .satisfied()).isFalse();
    }

    @Test
    void reportsEveryUnsatisfiedRule() {
        Rule satisfied = new Rule("amount", RuleOperator.GREATER_THAN, "10");
        Rule tooHigh = new Rule("amount", RuleOperator.LESS_THAN, "50");
        Rule missing = new Rule("approver", RuleOperator.EXISTS, null);

        RuleEvaluationResult result = evaluator.evaluate(List.of(satisfied, tooHigh, missing), Map.of("amount", 100));

        assertThat(result.satisfied()).isFalse();
        assertThat(result.unsatisfiedRules()).containsExactly(tooHigh, missing);
    }

    @Test
    void noRulesMeansSatisfied() {
        assertThat(evaluator.evaluate(List.of(), Map.of()).satisfied()).isTrue();
    }

    @Test
    void ruleRequiresFieldAndExpectedValueWhenOperatorNeedsOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Rule(" ", RuleOperator.EXISTS, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Rule("amount", RuleOperator.EQUALS, null));
        assertThat(new Rule("amount", RuleOperator.EQUALS, "10").describe()).isEqualTo("amount EQUALS 10");
        assertThat(new Rule("amount", RuleOperator.EXISTS, null).describe()).isEqualTo("amount EXISTS");
    }
}

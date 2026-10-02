package com.pablohenrique.workflowengine.rules.contract;

import java.util.List;
import java.util.Map;

/**
 * Contrato público do módulo Rules: responde se uma operação pode ser realizada.
 * A decisão sobre a mudança de estado permanece com quem solicita a avaliação.
 */
public interface RuleEvaluator {

    RuleEvaluationResult evaluate(List<Rule> rules, Map<String, Object> variables);
}

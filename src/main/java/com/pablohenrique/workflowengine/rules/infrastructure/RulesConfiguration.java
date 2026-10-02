package com.pablohenrique.workflowengine.rules.infrastructure;

import com.pablohenrique.workflowengine.rules.contract.RuleEvaluator;
import com.pablohenrique.workflowengine.rules.domain.DeterministicRuleEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RulesConfiguration {

    @Bean
    RuleEvaluator ruleEvaluator() {
        return new DeterministicRuleEvaluator();
    }
}

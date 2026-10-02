package com.pablohenrique.workflowengine.definition.contract;

import com.pablohenrique.workflowengine.rules.contract.Rule;

import java.util.List;

public record TransitionSnapshot(String action, String from, String to, String requiredRole, List<Rule> rules) {

    public TransitionSnapshot {
        rules = List.copyOf(rules);
    }
}

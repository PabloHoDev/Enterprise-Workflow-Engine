package com.pablohenrique.workflowengine.definition.application;

import com.pablohenrique.workflowengine.definition.domain.DefinitionException;

public class DefinitionNotFoundException extends DefinitionException {

    public DefinitionNotFoundException(String key) {
        super("Workflow definition '" + key + "' was not found");
    }
}

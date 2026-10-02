package com.pablohenrique.workflowengine.definition.application;

import com.pablohenrique.workflowengine.definition.domain.DefinitionException;

public class DefinitionKeyAlreadyExistsException extends DefinitionException {

    public DefinitionKeyAlreadyExistsException(String key) {
        super("Workflow definition key '" + key + "' is already in use");
    }
}

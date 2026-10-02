package com.pablohenrique.workflowengine.definition.domain;

public class DefinitionVersionNotFoundException extends DefinitionException {

    public DefinitionVersionNotFoundException(String definitionKey, int versionNumber) {
        super("Workflow definition '" + definitionKey + "' has no version " + versionNumber);
    }
}

package com.pablohenrique.workflowengine.definition.domain;

public class InvalidVersionStatusException extends DefinitionException {

    public InvalidVersionStatusException(String definitionKey, int versionNumber, VersionStatus current, String operation) {
        super("Version " + versionNumber + " of workflow definition '" + definitionKey
                + "' cannot be " + operation + " because it is " + current);
    }
}

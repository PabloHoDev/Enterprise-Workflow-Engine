package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.execution.domain.WorkflowException;

/** Não existe versão ativa da Workflow Definition para originar a execução (UC-006 A1). */
public class DefinitionUnavailableException extends WorkflowException {

    public DefinitionUnavailableException(String definitionKey) {
        super("Workflow definition '" + definitionKey + "' has no active version available for execution");
    }
}

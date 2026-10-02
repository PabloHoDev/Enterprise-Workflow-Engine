package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;

/** Filtros opcionais: campos {@code null} não restringem a busca. */
public record WorkflowSearchCriteria(String definitionKey, WorkflowStatus status) {
}

package com.pablohenrique.workflowengine.execution.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

final class WorkflowRequests {

    private WorkflowRequests() {
    }

    record CreateWorkflowRequest(@NotBlank @Size(max = 64) String definitionKey, Map<String, Object> variables) {
    }

    record ExecuteActionRequest(@NotBlank @Size(max = 64) String action, Map<String, Object> variables) {
    }

    record CancelWorkflowRequest(@Size(max = 500) String reason) {
    }
}

package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkflowSummary(UUID id, String definitionKey, int definitionVersion, WorkflowStatus status,
                              String currentState, Instant createdAt, Instant updatedAt) {
}

package com.pablohenrique.workflowengine.execution.interfaces.rest;

import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.execution.application.WorkflowSummary;
import com.pablohenrique.workflowengine.execution.domain.HistoryEntry;
import com.pablohenrique.workflowengine.execution.domain.HistoryEventType;
import com.pablohenrique.workflowengine.execution.domain.Workflow;
import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class WorkflowResponses {

    private WorkflowResponses() {
    }

    record WorkflowResponse(UUID id, String definitionKey, int definitionVersion, WorkflowStatus status,
                            String currentState, Map<String, Object> variables, Instant createdAt,
                            Instant updatedAt) {

        static WorkflowResponse from(Workflow workflow) {
            return new WorkflowResponse(workflow.id(), workflow.definitionKey(), workflow.definitionVersionNumber(),
                    workflow.status(), workflow.currentState(), workflow.variables(), workflow.createdAt(),
                    workflow.updatedAt());
        }
    }

    record WorkflowSummaryResponse(UUID id, String definitionKey, int definitionVersion, WorkflowStatus status,
                                   String currentState, Instant createdAt, Instant updatedAt) {

        static WorkflowSummaryResponse from(WorkflowSummary summary) {
            return new WorkflowSummaryResponse(summary.id(), summary.definitionKey(), summary.definitionVersion(),
                    summary.status(), summary.currentState(), summary.createdAt(), summary.updatedAt());
        }
    }

    record HistoryEntryResponse(int sequence, HistoryEventType type, String action, String fromState,
                                String toState, String actorId, Instant occurredAt, String comment) {

        static HistoryEntryResponse from(HistoryEntry entry) {
            return new HistoryEntryResponse(entry.sequence(), entry.type(), entry.action(), entry.fromState(),
                    entry.toState(), entry.actorId(), entry.occurredAt(), entry.comment());
        }
    }

    record AvailableActionResponse(String action, String targetState, String requiredRole) {

        static AvailableActionResponse from(TransitionSnapshot transition) {
            return new AvailableActionResponse(transition.action(), transition.to(), transition.requiredRole());
        }
    }
}

package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.definition.contract.DefinitionCatalog;
import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.execution.domain.Actor;
import com.pablohenrique.workflowengine.execution.domain.Workflow;
import com.pablohenrique.workflowengine.execution.domain.WorkflowException;
import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;
import com.pablohenrique.workflowengine.rules.contract.RuleEvaluator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private static final String RESOURCE_TYPE = "WORKFLOW";

    private final WorkflowRepository repository;
    private final DefinitionCatalog definitionCatalog;
    private final RuleEvaluator ruleEvaluator;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    WorkflowService(WorkflowRepository repository, DefinitionCatalog definitionCatalog, RuleEvaluator ruleEvaluator,
                    AuditRecorder auditRecorder, Clock clock) {
        this.repository = repository;
        this.definitionCatalog = definitionCatalog;
        this.ruleEvaluator = ruleEvaluator;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public Workflow create(String definitionKey, Map<String, Object> variables, Actor actor) {
        VersionSnapshot definition = definitionCatalog.findActiveVersion(definitionKey)
                .orElseThrow(() -> new DefinitionUnavailableException(definitionKey));
        Workflow workflow = Workflow.create(definition, variables, actor, clock.instant());
        repository.save(workflow);
        audit(AuditEvent.success(actor.id(), "WORKFLOW_CREATED", RESOURCE_TYPE, workflow.id().toString(),
                definitionKey + " v" + definition.versionNumber()));
        return workflow;
    }

    @Transactional
    public Workflow start(UUID id, Actor actor) {
        Workflow workflow = get(id);
        auditingRejections(actor, "WORKFLOW_STARTED", id, null, () -> workflow.start(actor, clock.instant()));
        repository.save(workflow);
        audit(AuditEvent.success(actor.id(), "WORKFLOW_STARTED", RESOURCE_TYPE, id.toString(), null));
        return workflow;
    }

    @Transactional
    public Workflow executeAction(UUID id, String action, Map<String, Object> variables, Actor actor) {
        Workflow workflow = get(id);
        VersionSnapshot definition = definitionOf(workflow);
        String previousState = workflow.currentState();
        auditingRejections(actor, "WORKFLOW_ACTION_EXECUTED", id, action,
                () -> workflow.execute(action, variables, definition, actor, ruleEvaluator, clock.instant()));
        repository.save(workflow);
        audit(AuditEvent.success(actor.id(), "WORKFLOW_ACTION_EXECUTED", RESOURCE_TYPE, id.toString(),
                action + ": " + previousState + " -> " + workflow.currentState()));
        return workflow;
    }

    @Transactional
    public Workflow cancel(UUID id, String reason, Actor actor) {
        Workflow workflow = get(id);
        auditingRejections(actor, "WORKFLOW_CANCELLED", id, null,
                () -> workflow.cancel(actor, reason, clock.instant()));
        repository.save(workflow);
        audit(AuditEvent.success(actor.id(), "WORKFLOW_CANCELLED", RESOURCE_TYPE, id.toString(), reason));
        return workflow;
    }

    @Transactional(readOnly = true)
    public Workflow get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new WorkflowNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TransitionSnapshot> availableTransitions(UUID id) {
        Workflow workflow = get(id);
        return workflow.availableTransitions(definitionOf(workflow));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowSummary> search(WorkflowSearchCriteria criteria, Pageable pageable) {
        return repository.search(criteria, pageable);
    }

    /** Quantidade de Workflows em cada status, incluindo os status sem nenhum Workflow. */
    @Transactional(readOnly = true)
    public Map<WorkflowStatus, Long> countByStatus() {
        Map<WorkflowStatus, Long> counts = new EnumMap<>(WorkflowStatus.class);
        for (WorkflowStatus status : WorkflowStatus.values()) {
            counts.put(status, 0L);
        }
        counts.putAll(repository.countByStatus());
        return counts;
    }

    private VersionSnapshot definitionOf(Workflow workflow) {
        return definitionCatalog.findVersion(workflow.definitionVersionId())
                .orElseThrow(() -> new IllegalStateException("Definition version " + workflow.definitionVersionId()
                        + " referenced by workflow " + workflow.id() + " no longer exists"));
    }

    /** Tentativas recusadas pelo domínio também são rastreáveis (BR-032), mesmo com o rollback da operação. */
    private void auditingRejections(Actor actor, String operation, UUID workflowId, String action,
                                    Runnable operationBody) {
        try {
            operationBody.run();
        } catch (WorkflowException rejection) {
            String detail = action == null ? rejection.getMessage() : action + ": " + rejection.getMessage();
            audit(AuditEvent.rejected(actor.id(), operation, RESOURCE_TYPE, workflowId.toString(), detail));
            throw rejection;
        }
    }

    private void audit(AuditEvent event) {
        auditRecorder.record(event);
    }
}

package com.pablohenrique.workflowengine.definition.application;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.definition.domain.DefinitionVersion;
import com.pablohenrique.workflowengine.definition.domain.StateDefinition;
import com.pablohenrique.workflowengine.definition.domain.TransitionDefinition;
import com.pablohenrique.workflowengine.definition.domain.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class DefinitionService {

    private static final String RESOURCE_TYPE = "WORKFLOW_DEFINITION";

    private final WorkflowDefinitionRepository repository;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    DefinitionService(WorkflowDefinitionRepository repository, AuditRecorder auditRecorder, Clock clock) {
        this.repository = repository;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public WorkflowDefinition create(String key, String name, String description, List<StateDefinition> states,
                                     List<TransitionDefinition> transitions, String actorId) {
        if (repository.existsByKey(key)) {
            throw new DefinitionKeyAlreadyExistsException(key);
        }
        WorkflowDefinition definition =
                WorkflowDefinition.create(key, name, description, states, transitions, clock.instant());
        repository.save(definition);
        auditRecorder.record(AuditEvent.success(actorId, "DEFINITION_CREATED", RESOURCE_TYPE, key, "version 1"));
        return definition;
    }

    @Transactional
    public DefinitionVersion addVersion(String key, List<StateDefinition> states,
                                        List<TransitionDefinition> transitions, String actorId) {
        WorkflowDefinition definition = get(key);
        DefinitionVersion version = definition.addVersion(states, transitions, clock.instant());
        repository.save(definition);
        auditRecorder.record(AuditEvent.success(actorId, "DEFINITION_VERSION_CREATED", RESOURCE_TYPE, key,
                "version " + version.number()));
        return version;
    }

    @Transactional
    public DefinitionVersion activate(String key, int versionNumber, String actorId) {
        WorkflowDefinition definition = get(key);
        DefinitionVersion version = definition.activate(versionNumber, clock.instant());
        repository.save(definition);
        auditRecorder.record(AuditEvent.success(actorId, "DEFINITION_VERSION_ACTIVATED", RESOURCE_TYPE, key,
                "version " + versionNumber));
        return version;
    }

    @Transactional
    public DefinitionVersion deactivate(String key, int versionNumber, String actorId) {
        WorkflowDefinition definition = get(key);
        DefinitionVersion version = definition.deactivate(versionNumber, clock.instant());
        repository.save(definition);
        auditRecorder.record(AuditEvent.success(actorId, "DEFINITION_VERSION_DEACTIVATED", RESOURCE_TYPE, key,
                "version " + versionNumber));
        return version;
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition get(String key) {
        return repository.findByKey(key).orElseThrow(() -> new DefinitionNotFoundException(key));
    }

    @Transactional(readOnly = true)
    public DefinitionVersion getVersion(String key, int versionNumber) {
        return get(key).requireVersion(versionNumber);
    }

    @Transactional(readOnly = true)
    public Page<DefinitionSummary> list(Pageable pageable) {
        return repository.findAll(pageable);
    }
}

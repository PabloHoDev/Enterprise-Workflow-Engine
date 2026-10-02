package com.pablohenrique.workflowengine.definition.application;

import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.definition.domain.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository {

    void save(WorkflowDefinition definition);

    boolean existsByKey(String key);

    Optional<WorkflowDefinition> findByKey(String key);

    Page<DefinitionSummary> findAll(Pageable pageable);

    Optional<VersionSnapshot> findActiveVersionSnapshot(String definitionKey);

    Optional<VersionSnapshot> findVersionSnapshot(UUID versionId);
}

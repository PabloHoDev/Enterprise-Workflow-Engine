package com.pablohenrique.workflowengine.definition.application;

import com.pablohenrique.workflowengine.definition.contract.DefinitionCatalog;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class DefinitionCatalogService implements DefinitionCatalog {

    private final WorkflowDefinitionRepository repository;

    DefinitionCatalogService(WorkflowDefinitionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<VersionSnapshot> findActiveVersion(String definitionKey) {
        return repository.findActiveVersionSnapshot(definitionKey);
    }

    @Override
    public Optional<VersionSnapshot> findVersion(UUID versionId) {
        return repository.findVersionSnapshot(versionId);
    }
}

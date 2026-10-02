package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import com.pablohenrique.workflowengine.definition.domain.VersionStatus;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

interface DefinitionVersionJpaRepository extends Repository<DefinitionVersionEntity, UUID> {

    Optional<DefinitionVersionEntity> findById(UUID id);

    Optional<DefinitionVersionEntity> findByDefinitionKeyAndStatus(String key, VersionStatus status);
}

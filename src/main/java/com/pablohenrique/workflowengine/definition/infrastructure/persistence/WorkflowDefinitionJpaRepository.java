package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface WorkflowDefinitionJpaRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {

    boolean existsByKey(String key);

    Optional<WorkflowDefinitionEntity> findByKey(String key);
}

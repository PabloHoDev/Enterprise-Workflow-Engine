package com.pablohenrique.workflowengine.execution.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.UUID;

interface WorkflowJpaRepository extends JpaRepository<WorkflowEntity, UUID>, JpaSpecificationExecutor<WorkflowEntity> {

    @Query("select w.status, count(w) from WorkflowEntity w group by w.status")
    List<Object[]> countByStatus();
}

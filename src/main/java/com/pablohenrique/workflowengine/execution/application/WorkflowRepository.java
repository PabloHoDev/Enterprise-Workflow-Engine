package com.pablohenrique.workflowengine.execution.application;

import com.pablohenrique.workflowengine.execution.domain.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository {

    void save(Workflow workflow);

    Optional<Workflow> findById(UUID id);

    Page<WorkflowSummary> search(WorkflowSearchCriteria criteria, Pageable pageable);
}

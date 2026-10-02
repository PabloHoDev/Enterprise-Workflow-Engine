package com.pablohenrique.workflowengine.execution.infrastructure.persistence;

import com.pablohenrique.workflowengine.execution.application.WorkflowRepository;
import com.pablohenrique.workflowengine.execution.application.WorkflowSearchCriteria;
import com.pablohenrique.workflowengine.execution.application.WorkflowSummary;
import com.pablohenrique.workflowengine.execution.domain.HistoryEntry;
import com.pablohenrique.workflowengine.execution.domain.Workflow;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWorkflowRepository implements WorkflowRepository {

    private final WorkflowJpaRepository workflows;

    JpaWorkflowRepository(WorkflowJpaRepository workflows) {
        this.workflows = workflows;
    }

    @Override
    public void save(Workflow workflow) {
        WorkflowEntity entity = workflows.findById(workflow.id()).orElse(null);
        if (entity == null) {
            entity = new WorkflowEntity(workflow.id(), workflow.definitionKey(), workflow.definitionVersionId(),
                    workflow.definitionVersionNumber(), workflow.status(), workflow.currentState(),
                    new LinkedHashMap<>(workflow.variables()), workflow.createdAt(), workflow.updatedAt());
            workflow.history().stream().map(this::toEntity).forEach(entity::addHistory);
            workflows.save(entity);
            return;
        }

        entity.update(workflow.status(), workflow.currentState(), new LinkedHashMap<>(workflow.variables()),
                workflow.updatedAt());
        // O History é append-only: só os registros ainda não persistidos são acrescentados.
        workflow.history().stream()
                .skip(entity.getHistory().size())
                .map(this::toEntity)
                .forEach(entity::addHistory);
    }

    @Override
    public Optional<Workflow> findById(UUID id) {
        return workflows.findById(id).map(this::toDomain);
    }

    @Override
    public Page<WorkflowSummary> search(WorkflowSearchCriteria criteria, Pageable pageable) {
        Specification<WorkflowEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.definitionKey() != null) {
                predicates.add(builder.equal(root.get("definitionKey"), criteria.definitionKey()));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return workflows.findAll(specification, pageable).map(entity -> new WorkflowSummary(entity.getId(),
                entity.getDefinitionKey(), entity.getDefinitionVersionNumber(), entity.getStatus(),
                entity.getCurrentState(), entity.getCreatedAt(), entity.getUpdatedAt()));
    }

    private WorkflowHistoryEntity toEntity(HistoryEntry entry) {
        return new WorkflowHistoryEntity(entry.sequence(), entry.type(), entry.action(), entry.fromState(),
                entry.toState(), entry.actorId(), entry.occurredAt(), entry.comment());
    }

    private Workflow toDomain(WorkflowEntity entity) {
        List<HistoryEntry> history = entity.getHistory().stream()
                .map(entry -> new HistoryEntry(entry.getSequence(), entry.getType(), entry.getAction(),
                        entry.getFromState(), entry.getToState(), entry.getActorId(), entry.getOccurredAt(),
                        entry.getComment()))
                .toList();
        return Workflow.restore(entity.getId(), entity.getDefinitionKey(), entity.getDefinitionVersionId(),
                entity.getDefinitionVersionNumber(), entity.getStatus(), entity.getCurrentState(),
                entity.getVariables(), entity.getCreatedAt(), entity.getUpdatedAt(), history);
    }
}

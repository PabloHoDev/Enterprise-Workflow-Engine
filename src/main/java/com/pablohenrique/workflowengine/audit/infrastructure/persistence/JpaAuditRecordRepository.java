package com.pablohenrique.workflowengine.audit.infrastructure.persistence;

import com.pablohenrique.workflowengine.audit.application.AuditRecordRepository;
import com.pablohenrique.workflowengine.audit.application.AuditSearchCriteria;
import com.pablohenrique.workflowengine.audit.domain.AuditRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
class JpaAuditRecordRepository implements AuditRecordRepository {

    private final AuditRecordJpaRepository records;

    JpaAuditRecordRepository(AuditRecordJpaRepository records) {
        this.records = records;
    }

    @Override
    public void save(AuditRecord record) {
        records.save(new AuditRecordEntity(record.id(), record.actorId(), record.operation(), record.resourceType(),
                record.resourceId(), record.outcome(), record.detail(), record.occurredAt()));
    }

    @Override
    public Page<AuditRecord> search(AuditSearchCriteria criteria, Pageable pageable) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("actorId", criteria.actorId());
        filters.put("operation", criteria.operation());
        filters.put("resourceType", criteria.resourceType());
        filters.put("resourceId", criteria.resourceId());
        filters.put("outcome", criteria.outcome());

        Specification<AuditRecordEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            filters.forEach((attribute, value) -> {
                if (value != null) {
                    predicates.add(builder.equal(root.get(attribute), value));
                }
            });
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return records.findAll(specification, pageable).map(entity -> new AuditRecord(entity.getId(),
                entity.getActorId(), entity.getOperation(), entity.getResourceType(), entity.getResourceId(),
                entity.getOutcome(), entity.getDetail(), entity.getOccurredAt()));
    }
}

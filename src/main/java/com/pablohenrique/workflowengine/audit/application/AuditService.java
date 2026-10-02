package com.pablohenrique.workflowengine.audit.application;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditOutcome;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.audit.domain.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

@Service
public class AuditService implements AuditRecorder {

    private final AuditRecordRepository repository;
    private final TransactionTemplate independentTransaction;
    private final Clock clock;

    AuditService(AuditRecordRepository repository, PlatformTransactionManager transactionManager, Clock clock) {
        this.repository = repository;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Override
    public void record(AuditEvent event) {
        AuditRecord record = new AuditRecord(UUID.randomUUID(), event.actorId(), event.operation(),
                event.resourceType(), event.resourceId(), event.outcome(), event.detail(), clock.instant());
        if (event.outcome() == AuditOutcome.REJECTED) {
            // A operação recusada sofrerá rollback; o registro da tentativa não pode ir junto.
            independentTransaction.executeWithoutResult(status -> repository.save(record));
        } else {
            repository.save(record);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditRecord> search(AuditSearchCriteria criteria, Pageable pageable) {
        return repository.search(criteria, pageable);
    }
}

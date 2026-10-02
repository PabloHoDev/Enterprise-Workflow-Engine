package com.pablohenrique.workflowengine.audit.application;

import com.pablohenrique.workflowengine.audit.domain.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditRecordRepository {

    void save(AuditRecord record);

    Page<AuditRecord> search(AuditSearchCriteria criteria, Pageable pageable);
}

package com.pablohenrique.workflowengine.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface AuditRecordJpaRepository
        extends JpaRepository<AuditRecordEntity, UUID>, JpaSpecificationExecutor<AuditRecordEntity> {
}

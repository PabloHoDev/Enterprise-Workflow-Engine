package com.pablohenrique.workflowengine.audit.interfaces.rest;

import com.pablohenrique.workflowengine.audit.application.AuditSearchCriteria;
import com.pablohenrique.workflowengine.audit.application.AuditService;
import com.pablohenrique.workflowengine.audit.contract.AuditOutcome;
import com.pablohenrique.workflowengine.audit.domain.AuditRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-records")
@Tag(name = "Audit", description = "Rastreabilidade de operações (acesso restrito a administradores)")
class AuditController {

    private final AuditService service;

    AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta registros de auditoria, com filtros opcionais (UC-011)")
    PagedModel<AuditRecordResponse> search(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) AuditOutcome outcome,
            @ParameterObject @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        AuditSearchCriteria criteria = new AuditSearchCriteria(actorId, operation, resourceType, resourceId, outcome);
        return new PagedModel<>(service.search(criteria, pageable).map(AuditRecordResponse::from));
    }

    record AuditRecordResponse(UUID id, String actorId, String operation, String resourceType, String resourceId,
                               AuditOutcome outcome, String detail, Instant occurredAt) {

        static AuditRecordResponse from(AuditRecord record) {
            return new AuditRecordResponse(record.id(), record.actorId(), record.operation(), record.resourceType(),
                    record.resourceId(), record.outcome(), record.detail(), record.occurredAt());
        }
    }
}

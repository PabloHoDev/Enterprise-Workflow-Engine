package com.pablohenrique.workflowengine.execution.interfaces.rest;

import com.pablohenrique.workflowengine.execution.application.WorkflowSearchCriteria;
import com.pablohenrique.workflowengine.execution.application.WorkflowService;
import com.pablohenrique.workflowengine.execution.domain.Actor;
import com.pablohenrique.workflowengine.execution.domain.Workflow;
import com.pablohenrique.workflowengine.execution.domain.WorkflowStatus;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowRequests.CancelWorkflowRequest;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowRequests.CreateWorkflowRequest;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowRequests.ExecuteActionRequest;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowResponses.AvailableActionResponse;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowResponses.HistoryEntryResponse;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowResponses.WorkflowResponse;
import com.pablohenrique.workflowengine.execution.interfaces.rest.WorkflowResponses.WorkflowSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows", description = "Execuções concretas de Workflow Definitions")
class WorkflowController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final WorkflowService service;

    WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um Workflow a partir da versão ativa da definição (UC-006)")
    ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request,
                                            Authentication authentication, UriComponentsBuilder uriBuilder) {
        Workflow workflow = service.create(request.definitionKey(), request.variables(), actor(authentication));
        URI location = uriBuilder.path("/api/v1/workflows/{id}").build(workflow.id());
        return ResponseEntity.created(location).body(WorkflowResponse.from(workflow));
    }

    @GetMapping
    @Operation(summary = "Lista Workflows, com filtros opcionais (UC-008)")
    PagedModel<WorkflowSummaryResponse> search(
            @RequestParam(required = false) String definitionKey,
            @RequestParam(required = false) WorkflowStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return new PagedModel<>(service.search(new WorkflowSearchCriteria(definitionKey, status), pageable)
                .map(WorkflowSummaryResponse::from));
    }

    @GetMapping("/summary")
    @Operation(summary = "Quantidade de Workflows por status")
    SummaryResponse summary() {
        Map<WorkflowStatus, Long> counts = service.countByStatus();
        return new SummaryResponse(counts.values().stream().mapToLong(Long::longValue).sum(), counts);
    }

    record SummaryResponse(long total, Map<WorkflowStatus, Long> byStatus) {
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um Workflow e o seu State atual (UC-008)")
    WorkflowResponse get(@PathVariable UUID id) {
        return WorkflowResponse.from(service.get(id));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Inicia a execução do Workflow (UC-007)")
    WorkflowResponse start(@PathVariable UUID id, Authentication authentication) {
        return WorkflowResponse.from(service.start(id, actor(authentication)));
    }

    @PostMapping("/{id}/actions")
    @Operation(summary = "Executa uma ação disponível no State atual (UC-009)")
    WorkflowResponse executeAction(@PathVariable UUID id, @Valid @RequestBody ExecuteActionRequest request,
                                   Authentication authentication) {
        return WorkflowResponse.from(
                service.executeAction(id, request.action(), request.variables(), actor(authentication)));
    }

    @GetMapping("/{id}/actions")
    @Operation(summary = "Lista as ações disponíveis a partir do State atual")
    List<AvailableActionResponse> availableActions(@PathVariable UUID id) {
        return service.availableTransitions(id).stream().map(AvailableActionResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o Workflow (UC-012)")
    WorkflowResponse cancel(@PathVariable UUID id, @Valid @RequestBody(required = false) CancelWorkflowRequest request,
                            Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return WorkflowResponse.from(service.cancel(id, reason, actor(authentication)));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Consulta a evolução do Workflow em ordem cronológica (UC-010)")
    List<HistoryEntryResponse> history(@PathVariable UUID id) {
        return service.get(id).history().stream().map(HistoryEntryResponse::from).toList();
    }

    private Actor actor(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toSet());
        return new Actor(authentication.getName(), roles);
    }
}

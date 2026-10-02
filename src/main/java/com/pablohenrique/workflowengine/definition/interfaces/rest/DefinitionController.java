package com.pablohenrique.workflowengine.definition.interfaces.rest;

import com.pablohenrique.workflowengine.definition.application.DefinitionService;
import com.pablohenrique.workflowengine.definition.domain.DefinitionVersion;
import com.pablohenrique.workflowengine.definition.domain.WorkflowDefinition;
import com.pablohenrique.workflowengine.definition.interfaces.rest.DefinitionRequests.CreateDefinitionRequest;
import com.pablohenrique.workflowengine.definition.interfaces.rest.DefinitionRequests.CreateVersionRequest;
import com.pablohenrique.workflowengine.definition.interfaces.rest.DefinitionResponses.DefinitionResponse;
import com.pablohenrique.workflowengine.definition.interfaces.rest.DefinitionResponses.DefinitionSummaryResponse;
import com.pablohenrique.workflowengine.definition.interfaces.rest.DefinitionResponses.VersionResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/workflow-definitions")
@Tag(name = "Workflow Definitions", description = "Modelos versionados de processos")
class DefinitionController {

    private final DefinitionService service;

    DefinitionController(DefinitionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria uma Workflow Definition com a sua primeira versão (UC-001)")
    ResponseEntity<DefinitionResponse> create(@Valid @RequestBody CreateDefinitionRequest request,
                                              Authentication authentication, UriComponentsBuilder uriBuilder) {
        WorkflowDefinition definition = service.create(request.key(), request.name(), request.description(),
                DefinitionRequests.toStates(request.states()), DefinitionRequests.toTransitions(request.transitions()),
                authentication.getName());
        URI location = uriBuilder.path("/api/v1/workflow-definitions/{key}").build(definition.key());
        return ResponseEntity.created(location).body(DefinitionResponse.from(definition));
    }

    @GetMapping
    @Operation(summary = "Lista as Workflow Definitions (UC-005)")
    PagedModel<DefinitionSummaryResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "key", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return new PagedModel<>(service.list(pageable).map(DefinitionSummaryResponse::from));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Consulta uma Workflow Definition e o resumo das suas versões (UC-005)")
    DefinitionResponse get(@PathVariable String key) {
        return DefinitionResponse.from(service.get(key));
    }

    @PostMapping("/{key}/versions")
    @Operation(summary = "Cria uma nova versão sem alterar as anteriores (UC-002)")
    ResponseEntity<VersionResponse> addVersion(@PathVariable String key,
                                               @Valid @RequestBody CreateVersionRequest request,
                                               Authentication authentication, UriComponentsBuilder uriBuilder) {
        DefinitionVersion version = service.addVersion(key, DefinitionRequests.toStates(request.states()),
                DefinitionRequests.toTransitions(request.transitions()), authentication.getName());
        URI location = uriBuilder.path("/api/v1/workflow-definitions/{key}/versions/{number}")
                .build(key, version.number());
        return ResponseEntity.created(location).body(VersionResponse.from(key, version));
    }

    @GetMapping("/{key}/versions/{number}")
    @Operation(summary = "Consulta a estrutura completa de uma versão (UC-005)")
    VersionResponse getVersion(@PathVariable String key, @PathVariable int number) {
        return VersionResponse.from(key, service.getVersion(key, number));
    }

    @PostMapping("/{key}/versions/{number}/activate")
    @Operation(summary = "Disponibiliza a versão para novas execuções (UC-003)")
    VersionResponse activate(@PathVariable String key, @PathVariable int number, Authentication authentication) {
        return VersionResponse.from(key, service.activate(key, number, authentication.getName()));
    }

    @PostMapping("/{key}/versions/{number}/deactivate")
    @Operation(summary = "Retira a versão de novas execuções (UC-004)")
    VersionResponse deactivate(@PathVariable String key, @PathVariable int number, Authentication authentication) {
        return VersionResponse.from(key, service.deactivate(key, number, authentication.getName()));
    }
}

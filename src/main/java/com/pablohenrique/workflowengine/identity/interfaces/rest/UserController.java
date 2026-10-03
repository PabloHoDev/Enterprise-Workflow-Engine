package com.pablohenrique.workflowengine.identity.interfaces.rest;

import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import com.pablohenrique.workflowengine.identity.domain.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Gestão de contas (acesso restrito a administradores)")
class UserController {

    private final UserAccountService service;
    private final Clock clock;

    UserController(UserAccountService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Lista as contas")
    PagedModel<UserResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return new PagedModel<>(service.list(pageable).map(this::toResponse));
    }

    @GetMapping("/{username}")
    @Operation(summary = "Consulta uma conta")
    UserResponse get(@PathVariable String username) {
        return toResponse(service.get(username));
    }

    @PostMapping
    @Operation(summary = "Cria uma conta")
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request, Authentication authentication,
                                        UriComponentsBuilder uriBuilder) {
        UserAccount account = service.create(request.username(), request.displayName(), request.password(),
                request.roles(), authentication.getName());
        return ResponseEntity.created(uriBuilder.path("/api/v1/users/{username}").build(account.username()))
                .body(toResponse(account));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Atualiza nome, papéis e situação; encerra as sessões se o acesso mudar")
    UserResponse update(@PathVariable String username, @Valid @RequestBody UpdateUserRequest request,
                        Authentication authentication) {
        return toResponse(service.update(username, request.displayName(), request.roles(), request.enabled(),
                authentication.getName()));
    }

    @PostMapping("/{username}/password")
    @Operation(summary = "Define uma nova senha; encerra as sessões da conta")
    ResponseEntity<Void> resetPassword(@PathVariable String username, @Valid @RequestBody ResetPasswordRequest request,
                                       Authentication authentication) {
        service.resetPassword(username, request.newPassword(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{username}/unlock")
    @Operation(summary = "Remove o bloqueio por tentativas de login")
    UserResponse unlock(@PathVariable String username, Authentication authentication) {
        return toResponse(service.unlock(username, authentication.getName()));
    }

    private UserResponse toResponse(UserAccount account) {
        Instant now = clock.instant();
        return new UserResponse(account.id(), account.username(), account.displayName(), account.roles(),
                account.enabled(), account.isLocked(now), account.isLocked(now) ? account.lockedUntil() : null,
                account.failedLoginAttempts(), account.lastLoginAt(), account.passwordChangedAt(),
                account.createdAt(), account.updatedAt());
    }

    record CreateUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(max = 128) String password,
            @NotEmpty Set<@NotBlank @Size(max = 32) String> roles) {
    }

    record UpdateUserRequest(
            @NotBlank @Size(max = 120) String displayName,
            @NotEmpty Set<@NotBlank @Size(max = 32) String> roles,
            @NotNull Boolean enabled) {
    }

    record ResetPasswordRequest(@NotBlank @Size(max = 128) String newPassword) {
    }

    record UserResponse(UUID id, String username, String displayName, Set<String> roles, boolean enabled,
                        boolean locked, Instant lockedUntil, int failedLoginAttempts, Instant lastLoginAt,
                        Instant passwordChangedAt, Instant createdAt, Instant updatedAt) {
    }
}

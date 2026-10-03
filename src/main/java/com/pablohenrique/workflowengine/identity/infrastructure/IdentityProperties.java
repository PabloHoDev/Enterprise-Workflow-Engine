package com.pablohenrique.workflowengine.identity.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Configuração do módulo Identity ({@code workflow-engine.identity.*}).
 *
 * @param seedUsers contas criadas na inicialização quando ainda não existem (ex.: o primeiro administrador)
 */
@Validated
@ConfigurationProperties("workflow-engine.identity")
record IdentityProperties(
        @DefaultValue List<@Valid SeedUser> seedUsers,
        @DefaultValue @Valid Lockout lockout,
        @DefaultValue @Valid LoginRateLimit loginRateLimit) {

    record SeedUser(@NotBlank String username, String displayName, @NotBlank String password,
                    @NotEmpty Set<@NotBlank String> roles) {

        String displayNameOrUsername() {
            return displayName == null || displayName.isBlank() ? username : displayName;
        }
    }

    /** Bloqueio de conta após senhas erradas consecutivas. */
    record Lockout(@DefaultValue("5") @Min(1) int maxFailedAttempts,
                   @DefaultValue("15m") @NotNull Duration duration) {
    }

    /** Limite de tentativas de login por origem. */
    record LoginRateLimit(@DefaultValue("10") @Min(1) int maxAttempts,
                          @DefaultValue("1m") @NotNull Duration window) {
    }
}

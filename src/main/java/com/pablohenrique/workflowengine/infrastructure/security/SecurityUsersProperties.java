package com.pablohenrique.workflowengine.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

/**
 * Usuários da API, fornecidos por configuração externa (DP-002, DP-003). A aplicação não sobe sem ao menos um.
 *
 * @param users senhas no formato do {@code DelegatingPasswordEncoder}, por exemplo {@code {bcrypt}$2a$10$...}
 */
@Validated
@ConfigurationProperties("workflow-engine.security")
record SecurityUsersProperties(@NotEmpty List<@Valid User> users) {

    record User(@NotBlank String username, @NotBlank String password, @NotEmpty Set<@NotBlank String> roles) {
    }
}

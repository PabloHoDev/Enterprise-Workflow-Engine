package com.pablohenrique.workflowengine.identity.infrastructure;

import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import com.pablohenrique.workflowengine.identity.domain.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Cria as contas da configuração de implantação e garante que exista ao menos um administrador ativo:
 * sem ele, ninguém conseguiria administrar o sistema, então a aplicação não inicia.
 */
@Component
class SeedUsersInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUsersInitializer.class);

    private final IdentityProperties properties;
    private final UserAccountService service;

    SeedUsersInitializer(IdentityProperties properties, UserAccountService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (IdentityProperties.SeedUser seed : properties.seedUsers()) {
            try {
                if (service.seed(seed.username(), seed.displayNameOrUsername(), seed.password(), seed.roles())) {
                    log.info("Seeded user '{}' with roles {}", seed.username(), seed.roles());
                    warnIfWeak(seed);
                }
            } catch (DataIntegrityViolationException concurrentSeed) {
                // Outra instância criou a mesma conta ao mesmo tempo.
                log.debug("User '{}' was seeded concurrently", seed.username());
            }
        }
        if (!service.hasEnabledAdministrator()) {
            throw new IllegalStateException("No enabled administrator exists. Configure one with "
                    + "workflow-engine.identity.seed-users (see docs/architecture/SECURITY.md)");
        }
    }

    private void warnIfWeak(IdentityProperties.SeedUser seed) {
        if (!seed.password().startsWith("{") && !PasswordPolicy.violations(seed.password(), seed.username()).isEmpty()) {
            log.warn("Seeded user '{}' has a password that does not meet the password policy. "
                    + "Acceptable only for local development", seed.username());
        }
    }
}

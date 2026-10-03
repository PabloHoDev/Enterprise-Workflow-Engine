package com.pablohenrique.workflowengine.identity.infrastructure;

import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.identity.application.LoginThrottle;
import com.pablohenrique.workflowengine.identity.application.SessionTerminator;
import com.pablohenrique.workflowengine.identity.application.UserAccountRepository;
import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import com.pablohenrique.workflowengine.identity.domain.LockoutPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
class IdentityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Novas senhas em BCrypt; o prefixo {id} permite migrar de algoritmo sem invalidar as existentes.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserAccountService userAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
                                          SessionTerminator sessionTerminator, AuditRecorder auditRecorder,
                                          IdentityProperties properties, Clock clock) {
        LockoutPolicy lockoutPolicy = new LockoutPolicy(properties.lockout().maxFailedAttempts(),
                properties.lockout().duration());
        return new UserAccountService(repository, passwordEncoder, sessionTerminator, auditRecorder, lockoutPolicy,
                clock);
    }

    @Bean
    LoginThrottle loginThrottle(IdentityProperties properties, Clock clock) {
        return new LoginThrottle(properties.loginRateLimit().maxAttempts(), properties.loginRateLimit().window(),
                clock);
    }
}

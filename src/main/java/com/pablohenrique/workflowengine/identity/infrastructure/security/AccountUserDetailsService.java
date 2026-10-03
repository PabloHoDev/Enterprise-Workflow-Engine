package com.pablohenrique.workflowengine.identity.infrastructure.security;

import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Ponte entre as contas do módulo Identity e a autenticação do Spring Security. */
@Component
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountService service;
    private final Clock clock;

    AccountUserDetailsService(UserAccountService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return service.findForAuthentication(username)
                .map(account -> User.withUsername(account.username())
                        .password(account.passwordHash())
                        .roles(account.roles().toArray(String[]::new))
                        .disabled(!account.enabled())
                        .accountLocked(account.isLocked(clock.instant()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}

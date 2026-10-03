package com.pablohenrique.workflowengine.identity.infrastructure.security;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Aplica o bloqueio por tentativas e audita falhas de autenticação, venham do console web ou de HTTP Basic.
 */
@Component
class AuthenticationEventsListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventsListener.class);
    private static final int MAX_AUDITED_USERNAME_LENGTH = 64;

    private final UserAccountService service;
    private final AuditRecorder auditRecorder;

    AuthenticationEventsListener(UserAccountService service, AuditRecorder auditRecorder) {
        this.service = service;
        this.auditRecorder = auditRecorder;
    }

    @EventListener
    void onSuccess(AuthenticationSuccessEvent event) {
        try {
            service.recordSuccessfulAuthentication(event.getAuthentication().getName());
        } catch (RuntimeException e) {
            // A autenticação já foi decidida; uma falha aqui não pode transformá-la em erro para o cliente.
            log.warn("Could not reset failed login counter", e);
        }
    }

    @EventListener
    void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = sanitize(event.getAuthentication().getName());
        try {
            if (event instanceof AuthenticationFailureBadCredentialsEvent) {
                service.recordFailedAuthentication(username);
            }
            auditRecorder.record(AuditEvent.rejected(username, "LOGIN_FAILED", "USER", username,
                    event.getException().getClass().getSimpleName()));
        } catch (RuntimeException e) {
            log.warn("Could not record failed authentication", e);
        }
    }

    /** O nome digitado é entrada não confiável: limita o tamanho e remove caracteres de controle. */
    private String sanitize(String username) {
        if (username == null || username.isBlank()) {
            return "(empty)";
        }
        String clean = username.replaceAll("\\p{Cntrl}", "");
        return clean.length() > MAX_AUDITED_USERNAME_LENGTH ? clean.substring(0, MAX_AUDITED_USERNAME_LENGTH) : clean;
    }
}

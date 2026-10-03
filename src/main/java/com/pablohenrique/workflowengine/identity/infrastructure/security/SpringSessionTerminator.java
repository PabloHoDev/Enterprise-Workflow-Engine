package com.pablohenrique.workflowengine.identity.infrastructure.security;

import com.pablohenrique.workflowengine.identity.application.SessionTerminator;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Remove do repositório de sessões (PostgreSQL) todas as sessões abertas por um usuário. */
@Component
class SpringSessionTerminator implements SessionTerminator {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    SpringSessionTerminator(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void terminateSessionsOf(String username) {
        sessions.findByPrincipalName(username).keySet().forEach(sessions::deleteById);
    }
}

package com.pablohenrique.workflowengine.identity.application;

/**
 * Encerra as sessões ativas de um usuário, para que mudanças de acesso (desativação, papéis, senha)
 * valham imediatamente, e não apenas no próximo login.
 */
public interface SessionTerminator {

    void terminateSessionsOf(String username);
}

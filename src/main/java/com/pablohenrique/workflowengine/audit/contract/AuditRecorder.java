package com.pablohenrique.workflowengine.audit.contract;

/**
 * Contrato público do módulo Audit.
 *
 * <p>Eventos {@link AuditOutcome#SUCCESS} participam da transação de quem chama: só existem se a operação
 * for efetivada. Eventos {@link AuditOutcome#REJECTED} são gravados em transação própria, para sobreviverem
 * ao rollback da operação recusada.
 */
public interface AuditRecorder {

    void record(AuditEvent event);
}

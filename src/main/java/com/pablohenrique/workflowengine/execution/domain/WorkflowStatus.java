package com.pablohenrique.workflowengine.execution.domain;

/**
 * Ciclo de vida da execução, independente dos States definidos pelo processo.
 */
public enum WorkflowStatus {

    /** Criado e posicionado no State inicial, aguardando início (UC-006). */
    CREATED(false),
    /** Em execução: aceita ações (UC-009). */
    RUNNING(false),
    /** Alcançou um State terminal da definição (BR-038). */
    COMPLETED(true),
    /** Encerrado por cancelamento (UC-012). */
    CANCELLED(true);

    private final boolean terminal;

    WorkflowStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}

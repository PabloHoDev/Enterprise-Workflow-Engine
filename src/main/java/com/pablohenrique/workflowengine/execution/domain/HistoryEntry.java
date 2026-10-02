package com.pablohenrique.workflowengine.execution.domain;

import java.time.Instant;

/**
 * Registro de uma mudança relevante na evolução do Workflow (BR-029 a BR-031).
 *
 * @param sequence posição cronológica dentro do Workflow, iniciando em 1
 * @param action   ação que disparou a Transition; {@code null} para eventos de ciclo de vida
 * @param comment  observação livre, como o motivo de um cancelamento
 */
public record HistoryEntry(int sequence, HistoryEventType type, String action, String fromState, String toState,
                           String actorId, Instant occurredAt, String comment) {
}

package com.pablohenrique.workflowengine.definition.application;

import java.time.Instant;
import java.util.UUID;

/** @param activeVersion número da versão ativa; {@code null} quando nenhuma está disponível para execução */
public record DefinitionSummary(UUID id, String key, String name, String description, Instant createdAt,
                                Instant updatedAt, Integer activeVersion) {
}

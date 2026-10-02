package com.pablohenrique.workflowengine.definition.application;

import java.time.Instant;
import java.util.UUID;

public record DefinitionSummary(UUID id, String key, String name, String description, Instant createdAt,
                                Instant updatedAt) {
}

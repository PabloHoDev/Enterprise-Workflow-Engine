package com.pablohenrique.workflowengine.definition.domain;

public enum VersionStatus {
    /** Criada, ainda não disponível para novas execuções. */
    DRAFT,
    /** Disponível para originar novos Workflows. */
    ACTIVE,
    /** Retirada de novas execuções; Workflows existentes continuam (RF-005). */
    INACTIVE
}

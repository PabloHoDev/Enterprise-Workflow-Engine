package com.pablohenrique.workflowengine.definition.contract;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público do módulo Workflow Definition: como um processo funciona.
 */
public interface DefinitionCatalog {

    /** Versão atualmente disponível para originar novos Workflows. */
    Optional<VersionSnapshot> findActiveVersion(String definitionKey);

    /** Versão específica, independentemente do status: execuções existentes continuam presas a ela (RNF-010). */
    Optional<VersionSnapshot> findVersion(UUID versionId);
}

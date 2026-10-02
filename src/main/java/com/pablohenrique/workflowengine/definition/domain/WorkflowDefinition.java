package com.pablohenrique.workflowengine.definition.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate root do módulo Workflow Definition: o modelo versionado de um processo.
 */
public final class WorkflowDefinition {

    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final UUID id;
    private final String key;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final List<DefinitionVersion> versions;
    private Instant updatedAt;

    private WorkflowDefinition(UUID id, String key, String name, String description, Instant createdAt,
                               Instant updatedAt, List<DefinitionVersion> versions) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.versions = new ArrayList<>(versions);
    }

    /** Cria a definição já com a sua primeira versão (UC-001). */
    public static WorkflowDefinition create(String key, String name, String description, List<StateDefinition> states,
                                            List<TransitionDefinition> transitions, Instant now) {
        List<String> violations = new ArrayList<>();
        if (key == null || key.length() > MAX_KEY_LENGTH || !KEY.matcher(key).matches()) {
            violations.add("key must be lowercase kebab-case with at most " + MAX_KEY_LENGTH + " characters");
        }
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            violations.add("name is required and must have at most " + MAX_NAME_LENGTH + " characters");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            violations.add("description must have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (!violations.isEmpty()) {
            throw new InvalidDefinitionException(violations);
        }
        DefinitionVersion firstVersion = DefinitionVersion.create(1, states, transitions, now);
        return new WorkflowDefinition(UUID.randomUUID(), key, name.trim(), description, now, now, List.of(firstVersion));
    }

    public static WorkflowDefinition restore(UUID id, String key, String name, String description, Instant createdAt,
                                             Instant updatedAt, List<DefinitionVersion> versions) {
        return new WorkflowDefinition(id, key, name, description, createdAt, updatedAt, versions);
    }

    /** Alterações de comportamento sempre resultam em uma nova versão; as anteriores não são tocadas (BR-003, BR-007). */
    public DefinitionVersion addVersion(List<StateDefinition> states, List<TransitionDefinition> transitions, Instant now) {
        int nextNumber = versions.stream().mapToInt(DefinitionVersion::number).max().orElse(0) + 1;
        DefinitionVersion version = DefinitionVersion.create(nextNumber, states, transitions, now);
        versions.add(version);
        updatedAt = now;
        return version;
    }

    /** Ativa a versão e retira a anteriormente ativa de novas execuções: no máximo uma versão ativa por definição (BR-041). */
    public DefinitionVersion activate(int versionNumber, Instant now) {
        DefinitionVersion version = requireVersion(versionNumber);
        if (version.status() == VersionStatus.ACTIVE) {
            throw new InvalidVersionStatusException(key, versionNumber, version.status(), "activated");
        }
        activeVersion().ifPresent(active -> active.changeStatus(VersionStatus.INACTIVE));
        version.changeStatus(VersionStatus.ACTIVE);
        updatedAt = now;
        return version;
    }

    public DefinitionVersion deactivate(int versionNumber, Instant now) {
        DefinitionVersion version = requireVersion(versionNumber);
        if (version.status() != VersionStatus.ACTIVE) {
            throw new InvalidVersionStatusException(key, versionNumber, version.status(), "deactivated");
        }
        version.changeStatus(VersionStatus.INACTIVE);
        updatedAt = now;
        return version;
    }

    public DefinitionVersion requireVersion(int versionNumber) {
        return versions.stream()
                .filter(version -> version.number() == versionNumber)
                .findFirst()
                .orElseThrow(() -> new DefinitionVersionNotFoundException(key, versionNumber));
    }

    public Optional<DefinitionVersion> activeVersion() {
        return versions.stream().filter(version -> version.status() == VersionStatus.ACTIVE).findFirst();
    }

    public UUID id() {
        return id;
    }

    public String key() {
        return key;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<DefinitionVersion> versions() {
        return Collections.unmodifiableList(versions);
    }
}

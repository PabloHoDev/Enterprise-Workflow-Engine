package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_definition")
class WorkflowDefinitionEntity {

    @Id
    private UUID id;

    @Column(name = "definition_key", nullable = false, updatable = false)
    private String key;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Protege as invariantes do aggregate (ex.: uma única versão ativa) contra alterações concorrentes.
    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<DefinitionVersionEntity> versions = new ArrayList<>();

    protected WorkflowDefinitionEntity() {
    }

    WorkflowDefinitionEntity(UUID id, String key, String name, String description, Instant createdAt,
                             Instant updatedAt) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void addVersion(DefinitionVersionEntity version) {
        version.setDefinition(this);
        versions.add(version);
    }

    void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getKey() {
        return key;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    List<DefinitionVersionEntity> getVersions() {
        return versions;
    }
}

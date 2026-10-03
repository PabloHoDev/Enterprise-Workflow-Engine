package com.pablohenrique.workflowengine.identity.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserAccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new HashSet<>();

    protected UserAccountEntity() {
    }

    UserAccountEntity(UUID id, String username, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.createdAt = createdAt;
    }

    void update(String displayName, String passwordHash, Set<String> roles, boolean enabled, int failedLoginAttempts,
                Instant lockedUntil, Instant lastLoginAt, Instant passwordChangedAt, Instant updatedAt) {
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = passwordChangedAt;
        this.updatedAt = updatedAt;
        if (!this.roles.equals(roles)) {
            this.roles.clear();
            this.roles.addAll(roles);
        }
    }

    UUID getId() {
        return id;
    }

    String getUsername() {
        return username;
    }

    String getDisplayName() {
        return displayName;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    boolean isEnabled() {
        return enabled;
    }

    int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    Instant getLockedUntil() {
        return lockedUntil;
    }

    Instant getLastLoginAt() {
        return lastLoginAt;
    }

    Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Set<String> getRoles() {
        return roles;
    }
}

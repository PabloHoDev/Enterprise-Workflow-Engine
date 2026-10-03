package com.pablohenrique.workflowengine.identity.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate root do módulo Identity: uma conta que pode se autenticar e agir como Actor.
 *
 * <p>A conta guarda apenas o hash da senha. A verificação da senha e a regra de bloqueio por tentativas
 * ficam aqui para valerem igualmente para o console web e para clientes de integração.
 */
public final class UserAccount {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._-]{2,63}");
    private static final Pattern ROLE = Pattern.compile("[A-Z][A-Z0-9_]{1,31}");
    private static final int MAX_DISPLAY_NAME_LENGTH = 120;

    private final UUID id;
    private final String username;
    private final Instant createdAt;
    private String displayName;
    private String passwordHash;
    private Set<String> roles;
    private boolean enabled;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private Instant updatedAt;

    private UserAccount(UUID id, String username, String displayName, String passwordHash, Set<String> roles,
                        boolean enabled, int failedLoginAttempts, Instant lockedUntil, Instant lastLoginAt,
                        Instant passwordChangedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.roles = new TreeSet<>(roles);
        this.enabled = enabled;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = passwordChangedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserAccount create(String username, String displayName, String passwordHash, Set<String> roles,
                                     Instant now) {
        List<String> violations = new ArrayList<>();
        if (username == null || !USERNAME.matcher(username).matches()) {
            violations.add("username must have 3 to 64 lowercase letters, digits, '.', '_' or '-'");
        }
        validateProfile(displayName, roles, violations);
        if (!violations.isEmpty()) {
            throw new InvalidUserException(violations);
        }
        return new UserAccount(UUID.randomUUID(), username, displayName.trim(), passwordHash, roles, true, 0, null,
                null, now, now, now);
    }

    public static UserAccount restore(UUID id, String username, String displayName, String passwordHash,
                                      Set<String> roles, boolean enabled, int failedLoginAttempts,
                                      Instant lockedUntil, Instant lastLoginAt, Instant passwordChangedAt,
                                      Instant createdAt, Instant updatedAt) {
        return new UserAccount(id, username, displayName, passwordHash, roles, enabled, failedLoginAttempts,
                lockedUntil, lastLoginAt, passwordChangedAt, createdAt, updatedAt);
    }

    private static void validateProfile(String displayName, Set<String> roles, List<String> violations) {
        if (displayName == null || displayName.isBlank() || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            violations.add("display name is required and must have at most " + MAX_DISPLAY_NAME_LENGTH
                    + " characters");
        }
        if (roles == null || roles.isEmpty()) {
            violations.add("at least one role is required");
            return;
        }
        for (String role : roles) {
            // O prefixo ROLE_ é reservado ao Spring Security, que o acrescenta ao montar as authorities.
            if (role == null || !ROLE.matcher(role).matches() || role.startsWith("ROLE_")) {
                violations.add("role '" + role + "' must be uppercase letters, digits or '_' (2 to 32 characters)");
            }
        }
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Registra uma senha errada. Ao atingir o limite da política, a conta fica bloqueada pelo período
     * definido e o contador recomeça.
     *
     * @return {@code true} quando esta falha bloqueou a conta
     */
    public boolean recordFailedLogin(LockoutPolicy policy, Instant now) {
        if (isLocked(now)) {
            return false;
        }
        failedLoginAttempts++;
        updatedAt = now;
        if (failedLoginAttempts >= policy.maxFailedAttempts()) {
            lockedUntil = now.plus(policy.lockDuration());
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    /** @return {@code true} quando havia contador de falhas ou bloqueio a limpar */
    public boolean clearFailedLogins(Instant now) {
        if (failedLoginAttempts == 0 && lockedUntil == null) {
            return false;
        }
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
        return true;
    }

    public void recordInteractiveLogin(Instant now) {
        clearFailedLogins(now);
        lastLoginAt = now;
        updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        passwordHash = newPasswordHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void updateProfile(String newDisplayName, Set<String> newRoles, boolean newEnabled, Instant now) {
        List<String> violations = new ArrayList<>();
        validateProfile(newDisplayName, newRoles, violations);
        if (!violations.isEmpty()) {
            throw new InvalidUserException(violations);
        }
        displayName = newDisplayName.trim();
        roles = new TreeSet<>(newRoles);
        enabled = newEnabled;
        updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<String> roles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean enabled() {
        return enabled;
    }

    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public Instant passwordChangedAt() {
        return passwordChangedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}

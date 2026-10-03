package com.pablohenrique.workflowengine.identity.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final LockoutPolicy POLICY = new LockoutPolicy(3, Duration.ofMinutes(15));

    private static UserAccount account() {
        return UserAccount.create("ana.silva", "Ana Silva", "{bcrypt}hash", Set.of("USER"), NOW);
    }

    @Test
    void isCreatedEnabledUnlockedAndWithoutLoginHistory() {
        UserAccount account = account();

        assertThat(account.id()).isNotNull();
        assertThat(account.enabled()).isTrue();
        assertThat(account.isLocked(NOW)).isFalse();
        assertThat(account.failedLoginAttempts()).isZero();
        assertThat(account.lastLoginAt()).isNull();
        assertThat(account.passwordChangedAt()).isEqualTo(NOW);
        assertThat(account.roles()).containsExactly("USER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "Ana", "ana silva", "-ana", "ana@corp"})
    void rejectsInvalidUsernames(String username) {
        assertThatThrownBy(() -> UserAccount.create(username, "Ana", "hash", Set.of("USER"), NOW))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void rejectsMissingDisplayNameAndInvalidRoles() {
        InvalidUserException exception = catchThrowableOfType(InvalidUserException.class,
                () -> UserAccount.create("ana", " ", "hash", Set.of("user", "ROLE_ADMIN"), NOW));

        assertThat(exception.violations()).hasSize(3);
        assertThatThrownBy(() -> UserAccount.create("ana", "Ana", "hash", Set.of(), NOW))
                .isInstanceOf(InvalidUserException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void locksAfterTheMaximumConsecutiveFailures() {
        UserAccount account = account();

        assertThat(account.recordFailedLogin(POLICY, NOW)).isFalse();
        assertThat(account.recordFailedLogin(POLICY, NOW)).isFalse();
        assertThat(account.recordFailedLogin(POLICY, NOW)).isTrue();

        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(account.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void failuresWhileLockedDoNotExtendTheLock() {
        UserAccount account = account();
        for (int i = 0; i < 3; i++) {
            account.recordFailedLogin(POLICY, NOW);
        }

        assertThat(account.recordFailedLogin(POLICY, NOW.plusSeconds(60))).isFalse();
        assertThat(account.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void successfulLoginClearsFailuresAndRecordsTheAccess() {
        UserAccount account = account();
        account.recordFailedLogin(POLICY, NOW);

        account.recordInteractiveLogin(NOW.plusSeconds(5));

        assertThat(account.failedLoginAttempts()).isZero();
        assertThat(account.lastLoginAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(account.clearFailedLogins(NOW)).isFalse();
    }

    @Test
    void passwordChangeUnlocksTheAccount() {
        UserAccount account = account();
        for (int i = 0; i < 3; i++) {
            account.recordFailedLogin(POLICY, NOW);
        }

        account.changePassword("{bcrypt}new", NOW.plusSeconds(10));

        assertThat(account.isLocked(NOW.plusSeconds(10))).isFalse();
        assertThat(account.passwordHash()).isEqualTo("{bcrypt}new");
        assertThat(account.passwordChangedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void profileUpdateValidatesAndReplacesRoles() {
        UserAccount account = account();

        account.updateProfile(" Ana S. ", Set.of("USER", "MANAGER"), false, NOW.plusSeconds(1));

        assertThat(account.displayName()).isEqualTo("Ana S.");
        assertThat(account.roles()).containsExactly("MANAGER", "USER");
        assertThat(account.hasRole("MANAGER")).isTrue();
        assertThat(account.enabled()).isFalse();
        assertThat(account.updatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> account.updateProfile("Ana", Set.of("bad role"), true, NOW))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void lockoutPolicyRequiresPositiveValues() {
        assertThatThrownBy(() -> new LockoutPolicy(0, Duration.ofMinutes(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LockoutPolicy(1, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}

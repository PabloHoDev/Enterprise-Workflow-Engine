package com.pablohenrique.workflowengine.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsLongPassphrasesWithoutCompositionRules() {
        assertThat(PasswordPolicy.violations("correct horse battery staple", "ana")).isEmpty();
        assertThat(PasswordPolicy.violations("workflow-demo-2026", "manager")).isEmpty();
    }

    @Test
    void rejectsShortRepetitiveOversizedOrUsernameBasedPasswords() {
        assertThat(PasswordPolicy.violations("short", "ana")).containsExactly("password must have at least 12 characters");
        assertThat(PasswordPolicy.violations(null, "ana")).hasSize(1);
        assertThat(PasswordPolicy.violations("aaaaaaaaaaaaaaa", "ana"))
                .containsExactly("password must not repeat the same few characters");
        assertThat(PasswordPolicy.violations("x".repeat(60) + "yzw" + "q".repeat(70), "ana"))
                .contains("password must have at most 128 characters");
        assertThat(PasswordPolicy.violations("my-ANA.silva-2026", "ana.silva"))
                .containsExactly("password must not contain the username");
    }

    @Test
    void requireAcceptableThrowsWithEveryViolation() {
        assertThatThrownBy(() -> PasswordPolicy.requireAcceptable("short", "ana"))
                .isInstanceOf(WeakPasswordException.class)
                .satisfies(e -> assertThat(((WeakPasswordException) e).violations()).hasSize(1));
    }
}

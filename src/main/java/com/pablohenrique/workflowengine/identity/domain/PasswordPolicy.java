package com.pablohenrique.workflowengine.identity.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Política de senha alinhada ao NIST SP 800-63B: comprimento mínimo alto, sem regras de composição
 * obrigatória e recusando senhas trivialmente adivinháveis.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static List<String> violations(String password, String username) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("password must have at least " + MIN_LENGTH + " characters");
            return violations;
        }
        if (password.length() > MAX_LENGTH) {
            violations.add("password must have at most " + MAX_LENGTH + " characters");
        }
        if (password.chars().distinct().count() < 4) {
            violations.add("password must not repeat the same few characters");
        }
        if (username != null && password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            violations.add("password must not contain the username");
        }
        return violations;
    }

    public static void requireAcceptable(String password, String username) {
        List<String> violations = violations(password, username);
        if (!violations.isEmpty()) {
            throw new WeakPasswordException(violations);
        }
    }
}

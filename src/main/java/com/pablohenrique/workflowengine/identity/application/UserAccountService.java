package com.pablohenrique.workflowengine.identity.application;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import com.pablohenrique.workflowengine.identity.application.IdentityOperationException.Reason;
import com.pablohenrique.workflowengine.identity.domain.LockoutPolicy;
import com.pablohenrique.workflowengine.identity.domain.PasswordPolicy;
import com.pablohenrique.workflowengine.identity.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

/**
 * Casos de uso do módulo Identity: gestão de contas pelo administrador, troca de senha pelo próprio
 * usuário e registro das tentativas de login.
 */
public class UserAccountService {

    public static final String ADMIN_ROLE = "ADMIN";
    private static final String RESOURCE_TYPE = "USER";

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTerminator sessionTerminator;
    private final AuditRecorder auditRecorder;
    private final LockoutPolicy lockoutPolicy;
    private final Clock clock;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
                              SessionTerminator sessionTerminator, AuditRecorder auditRecorder,
                              LockoutPolicy lockoutPolicy, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTerminator = sessionTerminator;
        this.auditRecorder = auditRecorder;
        this.lockoutPolicy = lockoutPolicy;
        this.clock = clock;
    }

    @Transactional
    public UserAccount create(String username, String displayName, String password, Set<String> roles,
                              String actorId) {
        if (repository.existsByUsername(username)) {
            throw new IdentityOperationException(Reason.USERNAME_TAKEN, "Username '" + username + "' is already in use");
        }
        PasswordPolicy.requireAcceptable(password, username);
        UserAccount account = UserAccount.create(username, displayName, passwordEncoder.encode(password), roles,
                clock.instant());
        repository.save(account);
        auditRecorder.record(AuditEvent.success(actorId, "USER_CREATED", RESOURCE_TYPE, username,
                "roles " + account.roles()));
        return account;
    }

    @Transactional
    public UserAccount update(String username, String displayName, Set<String> roles, boolean enabled,
                              String actorId) {
        UserAccount account = get(username);
        boolean losesAdministration = account.enabled() && account.hasRole(ADMIN_ROLE)
                && (!enabled || roles == null || !roles.contains(ADMIN_ROLE));
        if (losesAdministration && username.equals(actorId)) {
            throw new IdentityOperationException(Reason.SELF_LOCKOUT,
                    "Administrators cannot disable themselves or remove their own ADMIN role");
        }
        if (losesAdministration && repository.countEnabledWithRole(ADMIN_ROLE) <= 1) {
            throw new IdentityOperationException(Reason.LAST_ADMINISTRATOR,
                    "The operation would leave the system without an enabled administrator");
        }
        boolean accessChanged = account.enabled() != enabled || !account.roles().equals(roles);
        account.updateProfile(displayName, roles, enabled, clock.instant());
        repository.save(account);
        if (accessChanged) {
            sessionTerminator.terminateSessionsOf(username);
        }
        auditRecorder.record(AuditEvent.success(actorId, "USER_UPDATED", RESOURCE_TYPE, username,
                "roles " + account.roles() + ", enabled " + account.enabled()));
        return account;
    }

    @Transactional
    public void resetPassword(String username, String newPassword, String actorId) {
        UserAccount account = get(username);
        PasswordPolicy.requireAcceptable(newPassword, username);
        account.changePassword(passwordEncoder.encode(newPassword), clock.instant());
        repository.save(account);
        sessionTerminator.terminateSessionsOf(username);
        auditRecorder.record(AuditEvent.success(actorId, "USER_PASSWORD_RESET", RESOURCE_TYPE, username, null));
    }

    /** Troca de senha pelo próprio usuário: exige a senha atual e encerra todas as suas sessões. */
    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        UserAccount account = get(username);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.passwordHash())) {
            auditRecorder.record(AuditEvent.rejected(username, "USER_PASSWORD_CHANGED", RESOURCE_TYPE, username,
                    "current password did not match"));
            throw new IdentityOperationException(Reason.WRONG_CURRENT_PASSWORD, "Current password is incorrect");
        }
        PasswordPolicy.requireAcceptable(newPassword, username);
        account.changePassword(passwordEncoder.encode(newPassword), clock.instant());
        repository.save(account);
        sessionTerminator.terminateSessionsOf(username);
        auditRecorder.record(AuditEvent.success(username, "USER_PASSWORD_CHANGED", RESOURCE_TYPE, username, null));
    }

    @Transactional
    public UserAccount unlock(String username, String actorId) {
        UserAccount account = get(username);
        account.clearFailedLogins(clock.instant());
        repository.save(account);
        auditRecorder.record(AuditEvent.success(actorId, "USER_UNLOCKED", RESOURCE_TYPE, username, null));
        return account;
    }

    @Transactional(readOnly = true)
    public UserAccount get(String username) {
        return repository.findByUsername(username).orElseThrow(() -> IdentityOperationException.userNotFound(username));
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findForAuthentication(String username) {
        return repository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean hasEnabledAdministrator() {
        return repository.countEnabledWithRole(ADMIN_ROLE) > 0;
    }

    /** Login pelo console web: além de limpar falhas, registra o momento do acesso. */
    @Transactional
    public void recordInteractiveLogin(String username) {
        repository.findByUsernameForUpdate(username).ifPresent(account -> {
            account.recordInteractiveLogin(clock.instant());
            repository.save(account);
        });
        auditRecorder.record(AuditEvent.success(username, "LOGIN_SUCCEEDED", RESOURCE_TYPE, username, "web session"));
    }

    /** Qualquer autenticação bem-sucedida zera o contador de falhas, sem escrita quando não há o que zerar. */
    @Transactional
    public void recordSuccessfulAuthentication(String username) {
        repository.findByUsername(username)
                .filter(account -> account.failedLoginAttempts() > 0 || account.lockedUntil() != null)
                .flatMap(account -> repository.findByUsernameForUpdate(username))
                .ifPresent(account -> {
                    if (account.clearFailedLogins(clock.instant())) {
                        repository.save(account);
                    }
                });
    }

    @Transactional
    public void recordFailedAuthentication(String username) {
        repository.findByUsernameForUpdate(username).ifPresent(account -> {
            boolean locked = account.recordFailedLogin(lockoutPolicy, clock.instant());
            repository.save(account);
            if (locked) {
                auditRecorder.record(AuditEvent.success("system", "ACCOUNT_LOCKED", RESOURCE_TYPE, username,
                        "locked until " + account.lockedUntil() + " after "
                                + lockoutPolicy.maxFailedAttempts() + " failed attempts"));
            }
        });
    }

    /**
     * Cria a conta informada pela configuração de implantação, se ainda não existir. A senha pode vir pronta
     * no formato do {@code DelegatingPasswordEncoder} ({@code {bcrypt}...}) ou em texto, que é então cifrado.
     *
     * @return {@code true} quando a conta foi criada
     */
    @Transactional
    public boolean seed(String username, String displayName, String passwordOrHash, Set<String> roles) {
        if (repository.existsByUsername(username)) {
            return false;
        }
        String hash = passwordOrHash.startsWith("{") ? passwordOrHash : passwordEncoder.encode(passwordOrHash);
        repository.save(UserAccount.create(username, displayName, hash, roles, clock.instant()));
        auditRecorder.record(AuditEvent.success("system", "USER_CREATED", RESOURCE_TYPE, username,
                "seeded from configuration, roles " + roles));
        return true;
    }
}

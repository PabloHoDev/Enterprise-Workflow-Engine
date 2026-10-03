package com.pablohenrique.workflowengine.identity.infrastructure.persistence;

import com.pablohenrique.workflowengine.identity.application.UserAccountRepository;
import com.pablohenrique.workflowengine.identity.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaUserAccountRepository implements UserAccountRepository {

    private final UserAccountJpaRepository users;

    JpaUserAccountRepository(UserAccountJpaRepository users) {
        this.users = users;
    }

    @Override
    public void save(UserAccount account) {
        UserAccountEntity entity = users.findById(account.id())
                .orElseGet(() -> new UserAccountEntity(account.id(), account.username(), account.createdAt()));
        entity.update(account.displayName(), account.passwordHash(), account.roles(), account.enabled(),
                account.failedLoginAttempts(), account.lockedUntil(), account.lastLoginAt(),
                account.passwordChangedAt(), account.updatedAt());
        users.save(entity);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.existsByUsername(username);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return users.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsernameForUpdate(String username) {
        return users.findByUsernameForUpdate(username).map(this::toDomain);
    }

    @Override
    public Page<UserAccount> findAll(Pageable pageable) {
        return users.findAll(pageable).map(this::toDomain);
    }

    @Override
    public long countEnabledWithRole(String role) {
        return users.countEnabledWithRole(role);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return UserAccount.restore(entity.getId(), entity.getUsername(), entity.getDisplayName(),
                entity.getPasswordHash(), entity.getRoles(), entity.isEnabled(), entity.getFailedLoginAttempts(),
                entity.getLockedUntil(), entity.getLastLoginAt(), entity.getPasswordChangedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

package com.pablohenrique.workflowengine.identity.application;

import com.pablohenrique.workflowengine.identity.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserAccountRepository {

    void save(UserAccount account);

    boolean existsByUsername(String username);

    Optional<UserAccount> findByUsername(String username);

    /** Bloqueia o registro até o fim da transação: tentativas de login simultâneas não perdem contagem. */
    Optional<UserAccount> findByUsernameForUpdate(String username);

    Page<UserAccount> findAll(Pageable pageable);

    long countEnabledWithRole(String role);
}

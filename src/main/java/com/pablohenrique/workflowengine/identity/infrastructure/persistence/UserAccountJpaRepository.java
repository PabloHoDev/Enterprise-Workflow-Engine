package com.pablohenrique.workflowengine.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {

    boolean existsByUsername(String username);

    Optional<UserAccountEntity> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccountEntity u where u.username = :username")
    Optional<UserAccountEntity> findByUsernameForUpdate(@Param("username") String username);

    @Query("select count(u) from UserAccountEntity u join u.roles r where u.enabled = true and r = :role")
    long countEnabledWithRole(@Param("role") String role);
}

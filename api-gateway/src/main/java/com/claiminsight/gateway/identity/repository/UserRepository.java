package com.claiminsight.gateway.identity.repository;

import com.claiminsight.gateway.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** JPA repository for gateway user persistence. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

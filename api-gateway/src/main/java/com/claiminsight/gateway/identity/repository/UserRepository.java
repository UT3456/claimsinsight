package com.claiminsight.gateway.identity.repository;

import com.claiminsight.gateway.identity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** JPA repository for gateway user persistence. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Lookup that accepts either the username or the email address —
     * used at login so users can type whichever identifier they remember.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

package com.demo.repository;

import com.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Mock user mirror — populated by the api-gateway via POST /api/notifications/users/sync.
 *
 * <p>The gateway is the source of truth for identities; this service stores a
 * lean copy so role-based notification fan-out
 * ({@code SELECT userId FROM users WHERE role IN :roles}) reaches the same
 * userIds the gateway signs into JWTs. Without this mirror, dispatch is stuck
 * targeting the seeded mock userIds (1–10) — registrations after seed never
 * receive role-targeted notifications.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Idempotent upsert keyed on userId. Uses MySQL's
     * {@code ON DUPLICATE KEY UPDATE} so the gateway can call us with the same
     * payload safely on every login or re-sync. The gateway's userId is
     * honored verbatim (we deliberately bypass the entity's IDENTITY
     * strategy — the gateway is authoritative).
     */
    @Modifying
    @Query(value =
        "INSERT INTO users (user_id, name, email, role, is_active) " +
        "VALUES (:userId, :name, :email, :role, :isActive) " +
        "ON DUPLICATE KEY UPDATE " +
        "  name      = VALUES(name), " +
        "  email     = VALUES(email), " +
        "  role      = VALUES(role), " +
        "  is_active = VALUES(is_active)",
        nativeQuery = true)
    void upsert(@Param("userId")   Long userId,
                @Param("name")     String name,
                @Param("email")    String email,
                @Param("role")     String role,
                @Param("isActive") Boolean isActive);
}

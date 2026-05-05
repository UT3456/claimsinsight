package com.claiminsight.gateway.identity.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Handles JWT generation and validation.
 *
 * <p>Tokens are signed with HMAC-SHA256 using a Base64-encoded secret. All token
 * operations are synchronous and safe to call from reactive pipelines via
 * {@code Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())}.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long expiration;

    @Value("${application.security.jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // default 7 days

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a signed JWT for the given user.
     *
     * @param username the subject claim
     * @param role     the role claim (e.g. "ROLE_ADMIN")
     * @param userId   the user-id claim
     * @return compact JWT string
     */
    public String generateToken(String username, String role, Long userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("role",   role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation and claim extraction
    // -------------------------------------------------------------------------

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public long getExpiration() {
        return expiration;
    }

    /**
     * Generates an opaque (non-JWT) refresh token string.
     * The actual token value is a random UUID — the expiry is tracked in the DB row,
     * not inside the token itself.
     *
     * @return raw token string to store and return to the client
     */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    /** Returns the refresh token expiry as a LocalDateTime from now. */
    public LocalDateTime refreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(refreshExpiration / 1_000);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

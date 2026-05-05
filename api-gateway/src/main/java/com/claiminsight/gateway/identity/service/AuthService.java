package com.claiminsight.gateway.identity.service;

import com.claiminsight.gateway.exception.AccountLockedException;
import com.claiminsight.gateway.identity.dto.AuthResponseDTO;
import com.claiminsight.gateway.identity.dto.LoginRequestDTO;
import com.claiminsight.gateway.identity.dto.RegisterRequestDTO;
import com.claiminsight.gateway.identity.model.RefreshToken;
import com.claiminsight.gateway.identity.model.Role;
import com.claiminsight.gateway.identity.model.User;
import com.claiminsight.gateway.identity.notification.NotificationEmitter;
import com.claiminsight.gateway.identity.repository.RefreshTokenRepository;
import com.claiminsight.gateway.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final AuditService           auditService;
    private final NotificationEmitter    notificationEmitter;
    private final LoginAttemptService    loginAttemptService;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        // Generic message — never reveal which field already exists.
        // Checking both before throwing prevents a second round-trip to confirm the other.
        boolean usernameTaken = userRepository.existsByUsername(request.getUsername());
        boolean emailTaken    = userRepository.existsByEmail(request.getEmail());
        if (usernameTaken || emailTaken) {
            throw new IllegalArgumentException("This username or email is already registered.");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_CLAIMS_ANALYST;

        User user = User.builder()
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user '{}' with role {}", saved.getUsername(), saved.getRole());

        auditService.log(saved.getUsername(), saved.getId(), "REGISTER", "/api/auth/register",
                "{\"role\":\"" + saved.getRole().name() + "\"}");

        // Mirror into NotificationService's users table so role-based dispatch and
        // the frontend unread-count poll converge on one identity.
        notificationEmitter.emitUserSync(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
        notificationEmitter.emitUserRegistered(saved.getUsername(), saved.getRole().name());

        String accessToken  = jwtService.generateToken(saved.getUsername(), saved.getRole().name(), saved.getId());
        String refreshToken = issueRefreshToken(saved.getUsername());
        return buildResponse(saved, accessToken, refreshToken);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {

        String identifier = request.getUsername();

        // ── 1. Lockout check (before any DB query so we don't leak existence) ──
        loginAttemptService.checkLocked(identifier);

        // ── 2. User lookup — username OR email ─────────────────────────────────
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(identifier);
                    auditService.log(identifier, null, "LOGIN_FAILED", "/api/auth/login",
                            "{\"reason\":\"user not found\"}");
                    return new BadCredentialsException("Invalid username or password");
                });

        // ── 3. Account enabled ────────────────────────────────────────────────
        if (!user.isEnabled()) {
            auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", "/api/auth/login",
                    "{\"reason\":\"account disabled\"}");
            notificationEmitter.emitDisabledAccountLogin(user.getUsername());
            throw new BadCredentialsException("Account is disabled");
        }

        // ── 4. Password check ─────────────────────────────────────────────────
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(identifier);
            auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", "/api/auth/login",
                    "{\"reason\":\"wrong password\"}");
            throw new BadCredentialsException("Invalid username or password");
        }

        // ── 5. Success ────────────────────────────────────────────────────────
        loginAttemptService.recordSuccess(identifier);
        log.info("User '{}' authenticated successfully", user.getUsername());
        auditService.log(user.getUsername(), user.getId(), "LOGIN_SUCCESS", "/api/auth/login", null);

        // Back-fill: ensures pre-existing users are mirrored in NotificationService
        // on their next successful login. Idempotent server-side.
        notificationEmitter.emitUserSync(user.getId(), user.getName(), user.getEmail(), user.getRole());

        String accessToken  = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        String refreshToken = issueRefreshToken(user.getUsername());
        return buildResponse(user, accessToken, refreshToken);
    }

    // -------------------------------------------------------------------------
    // Token refresh
    // -------------------------------------------------------------------------

    @Transactional
    public AuthResponseDTO refresh(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (stored.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BadCredentialsException("Refresh token has expired — please log in again");
        }

        User user = userRepository.findByUsernameOrEmail(stored.getUsername(), stored.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));

        // Rotate: revoke old token, issue a new one (prevents replay)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccess  = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        String newRefresh = issueRefreshToken(user.getUsername());

        log.info("Access token refreshed for '{}'", user.getUsername());
        auditService.log(user.getUsername(), user.getId(), "TOKEN_REFRESH", "/api/auth/refresh", null);

        return buildResponse(user, newAccess, newRefresh);
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Transactional
    public void logout(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
        log.info("All refresh tokens revoked for '{}'", username);
        auditService.log(username, null, "LOGOUT", "/api/auth/logout", null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Revoke any existing tokens for the user then persist and return a fresh one. */
    private String issueRefreshToken(String username) {
        refreshTokenRepository.revokeAllByUsername(username);
        String value = jwtService.generateRefreshTokenValue();
        refreshTokenRepository.save(RefreshToken.builder()
                .token(value)
                .username(username)
                .expiresAt(jwtService.refreshTokenExpiry())
                .revoked(false)
                .build());
        return value;
    }

    private AuthResponseDTO buildResponse(User user, String accessToken, String refreshToken) {
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration() / 1_000)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .build();
    }
}

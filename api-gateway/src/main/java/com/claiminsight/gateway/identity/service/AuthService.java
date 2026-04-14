package com.claiminsight.gateway.identity.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.claiminsight.gateway.identity.dto.AuthResponseDTO;
import com.claiminsight.gateway.identity.dto.LoginRequestDTO;
import com.claiminsight.gateway.identity.dto.RegisterRequestDTO;
import com.claiminsight.gateway.identity.model.Role;
import com.claiminsight.gateway.identity.model.User;
import com.claiminsight.gateway.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;
    private final AuditService    auditService;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already registered");
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

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name(), saved.getId());
        return buildResponse(saved, token);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditService.log(request.getUsername(), null, "LOGIN_FAILED", "/api/auth/login",
                            "{\"reason\":\"user not found\"}");
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!user.isEnabled()) {
            auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", "/api/auth/login",
                    "{\"reason\":\"account disabled\"}");
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.log(user.getUsername(), user.getId(), "LOGIN_FAILED", "/api/auth/login",
                    "{\"reason\":\"wrong password\"}");
            throw new BadCredentialsException("Invalid username or password");
        }

        log.info("User '{}' authenticated successfully", user.getUsername());
        auditService.log(user.getUsername(), user.getId(), "LOGIN_SUCCESS", "/api/auth/login", null);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        return buildResponse(user, token);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AuthResponseDTO buildResponse(User user, String token) {
        return AuthResponseDTO.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration() / 1000)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .build();
    }
}

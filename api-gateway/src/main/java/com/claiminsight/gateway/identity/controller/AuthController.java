package com.claiminsight.gateway.identity.controller;

import com.claiminsight.gateway.identity.dto.AuthResponseDTO;
import com.claiminsight.gateway.identity.dto.LoginRequestDTO;
import com.claiminsight.gateway.identity.dto.RefreshRequestDTO;
import com.claiminsight.gateway.identity.dto.RegisterRequestDTO;
import com.claiminsight.gateway.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Auth endpoints embedded in the API Gateway.
 * All JPA calls are offloaded to a bounded-elastic thread pool so they
 * don't block the Netty event loop.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        return Mono.fromCallable(() -> authService.register(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        return Mono.fromCallable(() -> authService.login(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * Exchange a valid refresh token for a new access token + rotated refresh token.
     * The old refresh token is revoked on use (rotation prevents replay attacks).
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return Mono.fromCallable(() -> authService.refresh(request.getRefreshToken()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * Revokes all refresh tokens for the authenticated user.
     * The access token itself is stateless and cannot be invalidated server-side
     * — clients must discard it. Tokens expire within 24 h naturally.
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated")));
        }
        return Mono.fromRunnable(() -> authService.logout(username))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(ResponseEntity.ok(Map.of("message", "Logged out successfully")));
    }
}

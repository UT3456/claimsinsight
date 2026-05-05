package com.claiminsight.gateway.identity.controller;

import com.claiminsight.gateway.identity.dto.UpdateUserRequestDTO;
import com.claiminsight.gateway.identity.dto.UserDTO;
import com.claiminsight.gateway.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * Admin-only user management endpoints.
 * Path-level authorization is enforced in {@link com.claiminsight.gateway.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Mono<ResponseEntity<List<UserDTO>>> list() {
        return Mono.fromCallable(userService::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> userService.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO request,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "unknown";
        return Mono.fromCallable(() -> userService.update(id, request, actor))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, String>>> delete(
            @PathVariable Long id,
            Authentication auth) {
        String actor = auth != null ? auth.getName() : "unknown";
        return Mono.fromRunnable(() -> userService.delete(id, actor))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(ResponseEntity.ok(Map.of("message", "User deleted")));
    }
}

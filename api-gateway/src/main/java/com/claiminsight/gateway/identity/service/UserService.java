package com.claiminsight.gateway.identity.service;

import com.claiminsight.gateway.identity.dto.UpdateUserRequestDTO;
import com.claiminsight.gateway.identity.dto.UserDTO;
import com.claiminsight.gateway.identity.model.User;
import com.claiminsight.gateway.identity.repository.RefreshTokenRepository;
import com.claiminsight.gateway.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Read + admin-only mutations against the gateway_users table. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService           auditService;

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(UserDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("User " + id + " not found"));
    }

    @Transactional
    public UserDTO update(Long id, UpdateUserRequestDTO request, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User " + id + " not found"));

        StringBuilder change = new StringBuilder();

        if (request.getRole() != null && request.getRole() != user.getRole()) {
            change.append("role:").append(user.getRole()).append("->").append(request.getRole()).append(' ');
            user.setRole(request.getRole());
        }

        if (request.getEnabled() != null && request.getEnabled() != user.isEnabled()) {
            change.append("enabled:").append(user.isEnabled()).append("->").append(request.getEnabled()).append(' ');
            user.setEnabled(request.getEnabled());
            if (!request.getEnabled()) {
                // Revoke active sessions when disabling
                refreshTokenRepository.revokeAllByUsername(user.getUsername());
            }
        }

        User saved = userRepository.save(user);

        if (change.length() > 0) {
            auditService.log(actor, null, "USER_UPDATED", "/api/users/" + id,
                    "{\"target\":\"" + user.getUsername() + "\",\"change\":\"" + change.toString().trim() + "\"}");
            log.info("[user-admin] {} updated user {} — {}", actor, user.getUsername(), change.toString().trim());
        }

        return UserDTO.from(saved);
    }

    @Transactional
    public void delete(Long id, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User " + id + " not found"));

        // Safety: prevent admin from deleting their own account
        if (user.getUsername().equals(actor)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        // Revoke all refresh tokens before removing the row
        refreshTokenRepository.revokeAllByUsername(user.getUsername());
        userRepository.delete(user);

        auditService.log(actor, null, "USER_DELETED", "/api/users/" + id,
                "{\"target\":\"" + user.getUsername() + "\"}");
        log.info("[user-admin] {} deleted user {}", actor, user.getUsername());
    }
}

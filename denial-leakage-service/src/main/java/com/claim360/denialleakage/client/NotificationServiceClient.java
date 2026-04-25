package com.claim360.denialleakage.client;

import com.claim360.denialleakage.client.dto.NotificationDispatchRequestDTO;
import com.claim360.denialleakage.client.dto.NotificationRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for NotificationService.
 * <p>
 * Prefer {@link #dispatchNotification} for role-scoped alerts (e.g. MANAGER +
 * ANALYST on a denial pattern). Use {@link #createNotification} only when
 * targeting an individual userId.
 */
@FeignClient(name = "NotificationService", path = "/api/notifications")
public interface NotificationServiceClient {

    @PostMapping
    ResponseEntity<?> createNotification(@RequestBody NotificationRequestDTO request);

    @PostMapping("/dispatch")
    ResponseEntity<?> dispatchNotification(@RequestBody NotificationDispatchRequestDTO request);
}

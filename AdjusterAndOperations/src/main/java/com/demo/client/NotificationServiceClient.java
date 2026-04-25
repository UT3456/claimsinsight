package com.demo.client;

import com.demo.client.dto.NotificationDispatchRequestDTO;
import com.demo.client.dto.NotificationRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for NotificationService.
 * <p>
 * Prefer {@link #dispatchNotification} for operational alerts that concern a
 * role (e.g. all MANAGERs should know about a CRITICAL SLA breach). Use
 * {@link #createNotification} only when the recipient is a known individual
 * userId.
 */
@FeignClient(name = "NotificationService", path = "/api/notifications")
public interface NotificationServiceClient {

    @PostMapping
    ResponseEntity<?> createNotification(@RequestBody NotificationRequestDTO request);

    @PostMapping("/dispatch")
    ResponseEntity<?> dispatchNotification(@RequestBody NotificationDispatchRequestDTO request);
}

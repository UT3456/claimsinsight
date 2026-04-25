package com.claiminsight.ingestion.client;

import com.claiminsight.ingestion.client.dto.NotificationDispatchRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for NotificationService — used by the ingestion pipeline to
 * raise SYSTEM alerts when a feed rejects a claim or a data-quality anomaly
 * is detected. Only the role-targeted {@code /dispatch} endpoint is exposed
 * since ingestion has no reason to address an individual user.
 */
@FeignClient(name = "NotificationService", path = "/api/notifications")
public interface NotificationServiceClient {

    @PostMapping("/dispatch")
    ResponseEntity<?> dispatchNotification(@RequestBody NotificationDispatchRequestDTO request);
}

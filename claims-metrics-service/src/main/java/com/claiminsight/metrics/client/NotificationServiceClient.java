package com.claiminsight.metrics.client;

import com.claiminsight.metrics.client.dto.NotificationRequestDTO;
import com.claiminsight.metrics.client.dto.NotificationDispatchRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for NotificationService.
 * Fires PERFORMANCE alerts when calculated KPIs breach operational thresholds
 * (e.g. TAT &gt; 30 days, cycle time &gt; 60 days, loss ratio &gt; 1.0).
 */
@FeignClient(name = "NotificationService", path = "/api/notifications")
public interface NotificationServiceClient {

    @PostMapping
    ResponseEntity<?> createNotification(@RequestBody NotificationRequestDTO request);

    @PostMapping("/dispatch")
    ResponseEntity<?> dispatchNotification(@RequestBody NotificationDispatchRequestDTO request);
}

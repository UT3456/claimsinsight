package com.demo.controller;

import com.demo.services.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events stream — clients open
 *   GET /api/notifications/stream/{userId}
 * and receive a `notification` event every time a new row is persisted for
 * that userId. Replaces the 30 s polling loop.
 *
 * Note on auth: the API gateway already authenticates the request (via
 * Authorization header OR ?token= query param — see JwtSecurityContextRepository).
 * This service trusts the gateway. No second JWT check here.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamService streamService;

    @GetMapping(path = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long userId) {
        log.debug("[sse] new stream request for user {}", userId);
        return streamService.register(userId);
    }
}

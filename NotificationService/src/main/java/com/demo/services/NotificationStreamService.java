package com.demo.services;

import com.demo.entities.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory registry of active SSE connections per user.
 *
 * Holds a Map<userId, List<SseEmitter>> so a single user can have multiple
 * tabs open and each one gets its own stream. When a new notification is
 * persisted, broadcast() pushes it to every emitter for the target user.
 *
 * Heartbeat every 25 s keeps the connection alive through corporate proxies
 * that drop idle HTTP/1.1 connections after 30 s.
 */
@Slf4j
@Service
public class NotificationStreamService {

    /** 30-minute hard cap — browser will auto-reconnect afterward. */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1_000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public NotificationStreamService() {
        // Heartbeat every 25 s — sends a comment-only line that the browser ignores
        heartbeat.scheduleAtFixedRate(this::sendHeartbeat, 25, 25, TimeUnit.SECONDS);
    }

    /**
     * Registers a new SSE emitter for a user and returns it. The emitter is
     * automatically removed from the registry on completion / timeout / error.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable removeFn = () -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) emitters.remove(userId);
            }
        };
        emitter.onCompletion(removeFn);
        emitter.onTimeout(removeFn);
        emitter.onError(e -> {
            log.debug("[sse] error on user {} stream: {}", userId, e.getMessage());
            removeFn.run();
        });

        // Send an initial connected event so the client knows the stream is live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"userId\":" + userId + "}"));
        } catch (IOException e) {
            log.warn("[sse] initial send failed for user {}: {}", userId, e.getMessage());
            removeFn.run();
        }

        log.info("[sse] connected user {} ({} active streams)", userId, totalActiveStreams());
        return emitter;
    }

    /**
     * Pushes a single notification to every active stream for that user.
     * Safe to call from any thread; failed sends are silently dropped (the
     * onError handler will clean up the dead emitter on its own).
     */
    public void broadcast(Notification n) {
        if (n == null || n.getUserId() == null) return;
        List<SseEmitter> list = emitters.get(n.getUserId());
        if (list == null || list.isEmpty()) return;

        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("notification")
                .data(toJson(n));

        for (SseEmitter emitter : list) {
            try {
                emitter.send(event);
            } catch (Exception e) {
                // Disconnected client — onError will fire and clean up
                log.debug("[sse] dropping dead emitter for user {}: {}", n.getUserId(), e.getMessage());
            }
        }
    }

    /** Convenience for batch dispatches — broadcasts each notification. */
    public void broadcastAll(List<Notification> ns) {
        if (ns == null) return;
        ns.forEach(this::broadcast);
    }

    private void sendHeartbeat() {
        SseEmitter.SseEventBuilder ping = SseEmitter.event().comment("hb");
        emitters.values().forEach(list -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(ping);
                } catch (Exception ignored) { /* dead emitter — onError will clean up */ }
            }
        });
    }

    private int totalActiveStreams() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Hand-rolled tiny serializer to avoid pulling in ObjectMapper here and
     * keep this service free of any Jackson coupling. Notifications have a
     * small, fixed shape — adding a field requires updating this method.
     */
    private String toJson(Notification n) {
        return "{"
                + "\"notificationId\":" + n.getNotificationId() + ","
                + "\"userId\":"         + n.getUserId() + ","
                + "\"title\":\""        + escape(n.getTitle()) + "\","
                + "\"message\":\""      + escape(n.getMessage()) + "\","
                + "\"category\":\""     + (n.getCategory() == null ? "" : n.getCategory().name()) + "\","
                + "\"referenceId\":\""  + escape(n.getReferenceId()) + "\","
                + "\"status\":\""       + (n.getStatus() == null ? "" : n.getStatus().name()) + "\","
                + "\"createdDate\":\""  + (n.getCreatedDate() == null ? "" : n.getCreatedDate()) + "\""
                + "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}

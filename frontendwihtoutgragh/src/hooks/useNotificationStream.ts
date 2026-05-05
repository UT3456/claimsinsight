import { useEffect, useRef, useState } from 'react';
import { notificationsApi } from '../api/notificationsApi';
import { store }             from '../store';

/**
 * Real-time notification stream via Server-Sent Events.
 *
 * Connects to GET /api/notifications/stream/{userId} — the API gateway
 * accepts the JWT via ?token=<jwt> query param (EventSource can't set
 * custom headers). Each new notification arrives as an "notification"
 * event and bumps the unread count.
 *
 * Resilience:
 *  - Browser auto-reconnects EventSource on network blips (default 3 s)
 *  - On HARD failure (5 reconnect attempts), falls back to a 60 s polling
 *    interval so the badge still updates eventually
 *  - Gracefully closes on unmount or userId change
 *
 * Initial count is fetched once via REST so the badge populates immediately
 * — SSE only delivers FUTURE events.
 */

const STREAM_BASE_PATH    = '/api/notifications/stream';
const POLL_FALLBACK_MS    = 60_000;
const MAX_RECONNECT_TRIES = 5;

export function useNotificationStream(userId: number | null) {
  const [unreadCount, setUnreadCount] = useState(0);
  const eventSourceRef = useRef<EventSource | null>(null);
  const fallbackTimer  = useRef<ReturnType<typeof setInterval> | null>(null);
  const reconnectTries = useRef(0);

  useEffect(() => {
    if (!userId) {
      setUnreadCount(0);
      return;
    }

    let cancelled = false;

    const closeAll = () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (fallbackTimer.current) {
        clearInterval(fallbackTimer.current);
        fallbackTimer.current = null;
      }
    };

    const startPollingFallback = () => {
      if (fallbackTimer.current || cancelled) return;
      console.warn('[notifications] SSE failed — falling back to 60 s polling');
      fallbackTimer.current = setInterval(async () => {
        if (cancelled) return;
        try {
          const count = await notificationsApi.getUnreadCount(userId);
          setUnreadCount(count);
        } catch { /* keep last-known count on error */ }
      }, POLL_FALLBACK_MS);
    };

    const connect = () => {
      const token = store.getState().auth.token;
      if (!token) return;

      // Vite dev proxy forwards /api → gateway; in prod use absolute origin
      const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/api\/?$/, '');
      const url  = `${base}${STREAM_BASE_PATH}/${userId}?token=${encodeURIComponent(token)}`;

      const es = new EventSource(url);
      eventSourceRef.current = es;

      es.addEventListener('connected', () => {
        reconnectTries.current = 0;
        if (fallbackTimer.current) {
          clearInterval(fallbackTimer.current);
          fallbackTimer.current = null;
        }
      });

      es.addEventListener('notification', () => {
        // Bump the badge — server already filtered to this user
        setUnreadCount(c => c + 1);
      });

      es.onerror = () => {
        // Browser will auto-reconnect; we just track failures to know when
        // to give up and switch to polling
        reconnectTries.current += 1;
        if (reconnectTries.current >= MAX_RECONNECT_TRIES) {
          closeAll();
          startPollingFallback();
        }
      };
    };

    // Seed the badge with the current count, then open the stream
    notificationsApi.getUnreadCount(userId)
      .then(c => { if (!cancelled) setUnreadCount(c); })
      .catch(() => { /* fall through to stream — count starts at 0 */ });

    connect();

    return () => {
      cancelled = true;
      closeAll();
    };
  }, [userId]);

  return { unreadCount };
}

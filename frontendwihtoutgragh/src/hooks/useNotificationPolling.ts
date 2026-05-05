import { useEffect, useRef, useState } from 'react';
import { notificationsApi }            from '../api/notificationsApi';

/**
 * Bell-badge polling with consecutive-failure backoff.
 *
 * Steady state: poll every 30 s.
 * If NotificationService is down (consecutive errors), fall back to 2 minutes
 * so a sustained outage doesn't generate 120 useless requests per hour.
 * Recovery: a single successful poll resets the backoff to 30 s.
 *
 * On any failure we keep the last-known count visible rather than resetting
 * the badge to zero — better to show stale data than hide a real alert.
 */

const POLL_INTERVAL_NORMAL     = 30_000;  // 30 s when healthy
const POLL_INTERVAL_BACKED_OFF = 120_000; // 2 min when failing
const FAILURE_THRESHOLD        = 3;       // consecutive failures before backoff
// Delay the very first poll so it doesn't compete with Dashboard's 5 parallel
// API calls that fire on the same mount cycle.
const INITIAL_DELAY = 5_000; // 5 s

export function useNotificationPolling(userId: number | null) {
  const [unreadCount, setUnreadCount] = useState(0);
  const prevCount    = useRef(0);
  const failureCount = useRef(0);
  const timeoutId    = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!userId) {
      // No user yet (auth still hydrating). Wait — don't poll user 0/null.
      setUnreadCount(0);
      prevCount.current = 0;
      return;
    }

    let cancelled = false;

    const scheduleNext = (delay: number) => {
      if (cancelled) return;
      timeoutId.current = setTimeout(fetchCount, delay);
    };

    const fetchCount = async () => {
      try {
        const count = await notificationsApi.getUnreadCount(userId);
        if (cancelled) return;
        setUnreadCount(count);
        prevCount.current = count;
        failureCount.current = 0;     // recovery: reset backoff
        scheduleNext(POLL_INTERVAL_NORMAL);
      } catch {
        // Don't reset the badge — keep showing the last known count.
        // After threshold consecutive failures, slow down.
        failureCount.current += 1;
        const delay = failureCount.current >= FAILURE_THRESHOLD
          ? POLL_INTERVAL_BACKED_OFF
          : POLL_INTERVAL_NORMAL;
        scheduleNext(delay);
      }
    };

    // Delay the first fetch so it does not contend with the Dashboard's
    // parallel data requests that fire on the same render cycle.
    timeoutId.current = setTimeout(fetchCount, INITIAL_DELAY);

    return () => {
      cancelled = true;
      if (timeoutId.current) clearTimeout(timeoutId.current);
    };
  }, [userId]);

  return { unreadCount };
}

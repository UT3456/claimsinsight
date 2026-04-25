import { useEffect, useRef, useState } from 'react';
import { notificationsApi }            from '../api/notificationsApi';

const POLL_INTERVAL = 30_000; // 30 seconds

export function useNotificationPolling(userId: number | null) {
  const [unreadCount, setUnreadCount] = useState(0);
  const prevCount                     = useRef(0);

  useEffect(() => {
    if (!userId) return;

    const fetchCount = async () => {
      try {
        const count = await notificationsApi.getUnreadCount(userId);
        setUnreadCount(count);
        prevCount.current = count;
      } catch {
        // silently ignore — notification service might be down
      }
    };

    fetchCount();
    const id = setInterval(fetchCount, POLL_INTERVAL);
    return () => clearInterval(id);
  }, [userId]);

  return { unreadCount };
}

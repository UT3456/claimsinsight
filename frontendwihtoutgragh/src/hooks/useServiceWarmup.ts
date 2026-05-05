import { useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';
import { store }     from '../store';

/**
 * Fires a single lightweight GET to each backend service as soon as the
 * authenticated layout mounts. Purpose: Spring Boot services are configured
 * with `lazy-initialization: true`, meaning every bean is created on the
 * FIRST HTTP request — which makes that first request feel slow (2-3 s).
 * By sending tiny warmup pings right after login, beans are initialising
 * while the user is still reading the Dashboard shell, so the real data
 * loads land against already-warm services.
 *
 * Rules:
 *  - Fire-and-forget: failures are silently swallowed (service may not be up)
 *  - Runs only ONCE per browser session (module-level flag)
 *  - Uses a dedicated 4 s timeout — shorter than the main 8 s so warmup
 *    requests don't sit in the queue after the real requests have already
 *    returned
 */

// Module-level flag so navigating away and back doesn't re-fire the pings.
let warmedUp = false;

// One request per service that the Dashboard (or other early pages) depends
// on.  Use page=0&size=1 so the DB work is trivial but every layer of the
// service stack (controller → service → repo → JPA → Hikari → MySQL) wakes up.
const WARMUP_PATHS: string[] = [
  '/ingest',           // data-ingestion-service   :8082
  '/kpis',             // claims-metrics-service    :8083
  '/risk-scores',      // fraud-risk-service        :8090
  '/denial-patterns',  // denial-leakage-service    :8085
  '/costs',            // cost-reserve-service      :8089
  '/adjusters',        // AdjusterAndOperations     :8087
  '/reports',          // analytics-report-service  :8084
];

export function useServiceWarmup(): void {
  useEffect(() => {
    if (warmedUp) return;

    // Only warm up when we have a JWT — all routes require auth.
    const token = store.getState().auth.token;
    if (!token) return;

    warmedUp = true;

    // Dedicated small timeout so these don't block later real requests.
    const warmupOptions = { params: { page: 0, size: 1 }, timeout: 4_000 };

    // allSettled: one service being down must never prevent the others from
    // waking up, and must never surface an error to the user.
    Promise.allSettled(
      WARMUP_PATHS.map(path => axiosInstance.get(path, warmupOptions)),
    ).then(results => {
      const failed = results.filter(r => r.status === 'rejected').length;
      if (failed > 0) {
        // Non-critical — just lets a developer see which services didn't respond.
        console.debug(`[warmup] ${results.length - failed}/${results.length} services responded`);
      }
    });
  }, []);
}

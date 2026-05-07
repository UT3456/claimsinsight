import { useEffect, useReducer, useCallback } from 'react';
import { Alert, Skeleton } from 'antd';
import { RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import {
  PageHeader, KpiCard, DataCard, Badge, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import type { BadgeTone } from '../../../components/ui';

import { usersApi, type User }    from '../../../api/usersApi';
import { feedsApi }    from '../../../api/dataIngestionApi';
import { auditApi }    from '../../../api/auditApi';
import { settledArray } from '../../../utils/settled';
import { ROLE_LABELS } from '../../../utils/roles';
import type { AppRole } from '../../../utils/roles';

/**
 * AdminDashboard — first-screen view for ROLE_ADMIN.
 *
 * Per SRS Module 4.1 + 4.2: admins configure and monitor the system rather
 * than read analytics. This page shows system health (users, feeds, recent
 * activity) and links into the admin tabs for actual configuration.
 *
 * KPIs:    Total users · Active feeds · Failed feeds · Recent audit events
 * Panels:  User-by-role distribution · Feed status table · Recent admin actions
 */

interface RoleCount { role: AppRole; count: number }
interface FeedRow { feedId: number; sourceSystem: string; feedType: string; status: string; lastSyncDate: string | null }

interface State {
  loading: boolean;
  error: string | null;
  totalUsers: number;
  enabledUsers: number;
  activeFeeds: number;
  failedFeeds: number;
  recentEvents: number;
  roleBreakdown: RoleCount[];
  feeds: FeedRow[];
  recentAuditActions: { username: string; action: string; timestamp: string }[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  totalUsers: 0, enabledUsers: 0, activeFeeds: 0, failedFeeds: 0, recentEvents: 0,
  roleBreakdown: [], feeds: [], recentAuditActions: [],
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START': return { ...s, loading: true, error: null };
    case 'OK':    return { ...s, loading: false, ...a.payload };
    case 'ERR':   return { ...s, loading: false, error: a.payload };
    default:      return s;
  }
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [usersR, feedsR, auditR] = await Promise.allSettled([
        usersApi.getAll(),
        feedsApi.getAll(),
        auditApi.getLogs(0, 10),
      ]);
      const users = settledArray<User>(usersR);
      const feeds = settledArray<{ feedId: number; sourceSystem: string; feedType: string; status: string; lastSyncDate: string | null }>(feedsR);
      // auditR is a Page object (not array) — keep as-is.
      const audit = auditR.status === 'fulfilled' ? auditR.value : null;

      // ── User role breakdown ────────────────────────────────────────────
      const tally = new Map<string, number>();
      for (const u of users) tally.set(u.role, (tally.get(u.role) ?? 0) + 1);
      const roleBreakdown: RoleCount[] = Array.from(tally.entries())
        .map(([role, count]) => ({ role: role as AppRole, count }))
        .sort((a, b) => b.count - a.count);

      const enabledUsers = users.filter(u => u.enabled).length;
      const activeFeeds  = feeds.filter(f => f.status === 'ACTIVE').length;
      const failedFeeds  = feeds.filter(f => f.status === 'FAILED').length;

      // Sort feeds: FAILED first (need attention), then ACTIVE, then INACTIVE
      const orderedFeeds: FeedRow[] = [...feeds].sort((a, b) => {
        const pri = (s: string) => s === 'FAILED' ? 0 : s === 'ACTIVE' ? 1 : 2;
        return pri(a.status) - pri(b.status);
      });

      const recentAuditActions = (audit?.content ?? []).slice(0, 8).map(e => ({
        username: e.username ?? 'system',
        action: e.action,
        timestamp: e.timestamp,
      }));

      dispatch({
        type: 'OK',
        payload: {
          totalUsers:   users.length,
          enabledUsers,
          activeFeeds,
          failedFeeds,
          recentEvents: audit?.totalElements ?? 0,
          roleBreakdown,
          feeds: orderedFeeds,
          recentAuditActions,
        },
      });
    } catch (err) {
      dispatch({ type: 'ERR', payload: (err as { userMessage?: string }).userMessage ?? 'Failed to load dashboard.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const feedStatusTone: Record<string, BadgeTone> = {
    ACTIVE:   'green',
    INACTIVE: 'neutral',
    FAILED:   'red',
  };

  const fmtSync = (iso: string | null) => {
    if (!iso) return '—';
    const d = new Date(iso);
    return isNaN(d.getTime()) ? '—' : d.toLocaleString();
  };

  return (
    <div>
      <PageHeader
        title="System overview"
        subtitle="Users · feeds · audit · system health"
        actions={<DarkButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>Refresh</DarkButton>}
      />

      {state.error && (
        <Alert type="warning" showIcon closable message={state.error}
          style={{ marginBottom: 16, borderRadius: 8 }} />
      )}

      {state.loading ? (
        <div style={styles.kpiGrid}>
          {[0,1,2,3].map(i => <div key={i} style={styles.skel}><Skeleton active paragraph={{ rows: 2 }} title={false} /></div>)}
        </div>
      ) : (
        <div style={styles.kpiGrid}>
          <KpiCard label="Users"
            value={state.totalUsers.toLocaleString()}
            delta={`${state.enabledUsers} active`} deltaDirection="flat" />
          <KpiCard label="Active feeds"
            value={state.activeFeeds.toLocaleString()}
            delta={`${state.failedFeeds} failed`}
            deltaTone={state.failedFeeds > 0 ? 'down' : undefined}
            tone={state.failedFeeds > 0 ? 'warning' : 'default'} />
          <KpiCard label="Audit events"
            value={state.recentEvents.toLocaleString()}
            delta="all time" deltaDirection="flat" />
          <KpiCard label="Roles configured"
            value={state.roleBreakdown.length.toLocaleString()}
            delta={`of 6 system roles`} deltaDirection="flat" />
        </div>
      )}

      {/* User role breakdown + feeds */}
      <div style={styles.twoCol}>
        <DataCard
          title="Users by role"
          subtitle="Distribution across the system"
          headerRight={<a onClick={() => navigate('/admin/users')} style={styles.link}>Manage →</a>}
        >
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.roleBreakdown.length === 0 ? (
            <EmptyState title="No users" description="Users will appear as they register." />
          ) : (
            state.roleBreakdown.map((r, i) => {
              const max = Math.max(...state.roleBreakdown.map(x => x.count), 1);
              const pct = (r.count / max) * 100;
              const colors = [CHART.blue, CHART.teal, CHART.purple, CHART.amber, CHART.red, '#5B6B7B'];
              return (
                <div key={r.role} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span>{ROLE_LABELS[r.role] ?? r.role}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>{r.count}</span>
                  </div>
                  <div style={styles.barTrack}>
                    <div style={{ ...styles.barFill, width: `${pct}%`, background: colors[i % colors.length] }} />
                  </div>
                </div>
              );
            })
          )}
        </DataCard>

        <DataCard
          title="Recent audit events"
          subtitle="Latest 8 actions"
          headerRight={<a onClick={() => navigate('/admin/audit-logs')} style={styles.link}>View all →</a>}
        >
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.recentAuditActions.length === 0 ? (
            <EmptyState title="No recent events" description="Audit events appear as users interact." tone="positive" />
          ) : (
            <div style={styles.eventList}>
              {state.recentAuditActions.map((e, i) => (
                <div key={i} style={styles.eventRow}>
                  <span style={styles.eventUser}>{e.username}</span>
                  <Badge tone="blue">{e.action}</Badge>
                  <span style={styles.eventTime}>{e.timestamp.replace('T', ' ').slice(0, 16)}</span>
                </div>
              ))}
            </div>
          )}
        </DataCard>
      </div>

      {/* Feed status */}
      <div style={{ marginTop: 12 }}>
        <DataCard
          title="Data feeds"
          subtitle="Failed feeds appear first"
          headerRight={<a onClick={() => navigate('/ingestion/feeds')} style={styles.link}>Manage →</a>}
        >
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.feeds.length === 0 ? (
            <EmptyState title="No feeds registered" description="Register a source system to begin ingestion." />
          ) : (
            <div style={styles.feedList}>
              {state.feeds.map(f => (
                <div key={f.feedId} style={styles.feedRow}>
                  <div style={{ flex: 1 }}>
                    <div style={styles.feedName}>{f.sourceSystem}</div>
                    <div style={styles.feedMeta}>
                      <Badge tone="blue">{f.feedType}</Badge>
                      <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>
                        last sync {fmtSync(f.lastSyncDate)}
                      </span>
                    </div>
                  </div>
                  <Badge tone={feedStatusTone[f.status] ?? 'neutral'}>{f.status}</Badge>
                </div>
              ))}
            </div>
          )}
        </DataCard>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 12 },
  skel:    { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 10, padding: '16px 20px', minHeight: 88 },
  twoCol:  { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 },
  barRow:  { marginBottom: 10 },
  barHead: { display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 4 },
  barTrack:{ height: 8, background: 'var(--ci-bg-surface-2)', borderRadius: 4, overflow: 'hidden' },
  barFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
  eventList:{ display: 'flex', flexDirection: 'column', gap: 6 },
  eventRow:{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 11, padding: '6px 8px', background: 'var(--ci-bg-surface-2)', borderRadius: 6 },
  eventUser:{ fontWeight: 500, minWidth: 100, color: 'var(--ci-text-primary)' },
  eventTime:{ marginLeft: 'auto', color: 'var(--ci-text-muted)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' },
  feedList:{ display: 'flex', flexDirection: 'column', gap: 6 },
  feedRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '10px 12px', background: 'var(--ci-bg-surface-2)', borderRadius: 8 },
  feedName:{ fontSize: 13, fontWeight: 500, color: 'var(--ci-text-primary)' },
  feedMeta:{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 2 },
  link:    { fontSize: 11, color: 'var(--ci-primary)', cursor: 'pointer', textDecoration: 'none' },
};

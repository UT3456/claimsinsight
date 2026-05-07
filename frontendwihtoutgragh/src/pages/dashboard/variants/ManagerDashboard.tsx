import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Alert, Skeleton, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import {
  PageHeader, KpiCard, DataCard, Badge, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import type { BadgeTone } from '../../../components/ui';
import { useAuth } from '../../../hooks/useAuth';

import { ingestApi }      from '../../../api/dataIngestionApi';
import { claimsApi }      from '../../../api/claimsApi';
import { adjustersApi, slaApi } from '../../../api/adjustersApi';
import type { AdjusterPerformance } from '../../../api/adjustersApi';
import { settledArray }   from '../../../utils/settled';

/**
 * ManagerDashboard — first-screen view for ROLE_CLAIMS_MANAGER.
 *
 * Per SRS Module 4.6 (adjuster ops): a manager watches their team's TAT vs
 * SLA, cycle time, and workload distribution. The KPI row is built around
 * SLA targets, not portfolio totals; the page anchors on the adjuster
 * workload table so reassigning load is one click away.
 *
 * KPIs:    Open claims · Avg TAT vs SLA · SLA violations · Avg cycle time · Team volume
 * Panels:  TAT trend with dashed 10-day SLA reference · Adjuster workload table
 *          (with On Track / At Risk / Breached status badges)
 */

const SLA_TAT_DAYS = 10;

interface State {
  loading: boolean;
  error: string | null;
  openClaims: number;
  avgTat: number | null;
  avgCycle: number | null;
  slaViolations: number;
  teamVolume: number;
  weeklyTat: { week: string; tat: number }[];
  adjusters: AdjusterPerformance[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  openClaims: 0, avgTat: null, avgCycle: null, slaViolations: 0, teamVolume: 0,
  weeklyTat: [], adjusters: [],
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START': return { ...s, loading: true, error: null };
    case 'OK':    return { ...s, loading: false, ...a.payload };
    case 'ERR':   return { ...s, loading: false, error: a.payload };
    default:      return s;
  }
}

function mean(nums: number[]): number | null {
  if (nums.length === 0) return null;
  return nums.reduce((a, b) => a + b, 0) / nums.length;
}

/** TAT compliance status used in the workload table — drives the badge tone. */
function statusOf(slaRate: number): { label: string; tone: BadgeTone } {
  if (slaRate >= 90) return { label: 'On Track', tone: 'green' };
  if (slaRate >= 75) return { label: 'At Risk',  tone: 'amber' };
  return                { label: 'Breached',  tone: 'red' };
}

export default function ManagerDashboard() {
  const { username } = useAuth();
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [rawR, tatR, cycR, adjR, slaR] = await Promise.allSettled([
        ingestApi.getAll(),
        claimsApi.getByMetric('TAT'),
        claimsApi.getByMetric('CYCLE_TIME'),
        adjustersApi.getAll(),
        slaApi.getAll(),
      ]);
      const raw       = settledArray<unknown>(rawR);
      const tats      = settledArray<{ metricDate: string; metricValue: number }>(tatR);
      const cycles    = settledArray<{ metricValue: number }>(cycR);
      const adjusters = settledArray<AdjusterPerformance>(adjR);
      const slas      = settledArray<unknown>(slaR);

      // ── Weekly TAT trend (last 8 weeks) ─────────────────────────────
      const buckets = new Map<string, number[]>();
      const now = new Date();
      const keys: string[] = [];
      for (let i = 7; i >= 0; i--) {
        const d = new Date(now); d.setDate(d.getDate() - i * 7);
        const k = `${d.getMonth() + 1}/${d.getDate()}`;
        keys.push(k);
        buckets.set(k, []);
      }
      for (const t of tats) {
        const d = new Date(t.metricDate);
        if (isNaN(d.getTime())) continue;
        const diffWeeks = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24 * 7));
        if (diffWeeks < 0 || diffWeeks > 7) continue;
        const k = keys[7 - diffWeeks];
        buckets.get(k)?.push(t.metricValue);
      }
      const weeklyTat = keys.map(k => {
        const xs = buckets.get(k) ?? [];
        return { week: k, tat: xs.length ? Math.round((xs.reduce((a, b) => a + b, 0) / xs.length) * 10) / 10 : 0 };
      });

      // Most-recent quarter per adjuster wins — collapse multi-period dataset
      const latest = new Map<number, AdjusterPerformance>();
      for (const a of adjusters) {
        const prev = latest.get(a.adjusterId ?? 0);
        if (!prev || (a.period ?? '') > (prev.period ?? '')) latest.set(a.adjusterId ?? 0, a);
      }
      const topAdjusters = Array.from(latest.values())
        .sort((a, b) => (b.claimsHandled ?? 0) - (a.claimsHandled ?? 0))
        .slice(0, 8);

      dispatch({
        type: 'OK',
        payload: {
          openClaims:    raw.length,
          avgTat:        mean(tats.map(t => t.metricValue)),
          avgCycle:      mean(cycles.map(c => c.metricValue)),
          slaViolations: slas.length,
          teamVolume:    Array.from(latest.values()).reduce((s, a) => s + (a.claimsHandled ?? 0), 0),
          weeklyTat,
          adjusters: topAdjusters,
        },
      });
    } catch (err) {
      dispatch({ type: 'ERR', payload: (err as { userMessage?: string }).userMessage ?? 'Failed to load dashboard.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const firstName = useMemo(() => (username ?? 'Manager').split(/[\s.]/)[0], [username]);
  const tatVsSla = state.avgTat !== null ? state.avgTat - SLA_TAT_DAYS : null;

  const adjusterCols: ColumnsType<AdjusterPerformance> = [
    {
      title: 'Adjuster', dataIndex: 'adjusterId', key: 'adjusterId', width: 120,
      render: (id: number) => <a onClick={() => navigate(`/adjusters/${id}`)} style={{ fontWeight: 500 }}>#{id}</a>,
    },
    {
      title: 'Claims', dataIndex: 'claimsHandled', key: 'claimsHandled', width: 80, align: 'right',
      sorter: (a, b) => (a.claimsHandled ?? 0) - (b.claimsHandled ?? 0),
    },
    {
      title: 'Avg TAT', dataIndex: 'avgTat', key: 'avgTat', width: 90, align: 'right',
      render: v => (
        <span style={{ color: (v ?? 0) > SLA_TAT_DAYS ? 'var(--ci-warning-text)' : 'var(--ci-text-secondary)' }}>
          {Number(v ?? 0).toFixed(1)}d
        </span>
      ),
    },
    {
      title: 'SLA %', dataIndex: 'slaComplianceRate', key: 'sla', width: 80, align: 'right',
      sorter: (a, b) => (a.slaComplianceRate ?? 0) - (b.slaComplianceRate ?? 0),
      render: (v: number) => `${Math.round(v ?? 0)}%`,
    },
    {
      title: 'Status', key: 'status', width: 100,
      render: (_, rec) => {
        const s = statusOf(rec.slaComplianceRate ?? 0);
        return <Badge tone={s.tone}>{s.label}</Badge>;
      },
    },
    {
      title: 'Period', dataIndex: 'period', key: 'period', width: 80,
      render: v => <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>{v}</span>,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Team performance"
        subtitle={`${firstName} · workload · TAT vs ${SLA_TAT_DAYS}-day SLA`}
        actions={<DarkButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>Refresh</DarkButton>}
      />

      {state.error && (
        <Alert type="warning" showIcon closable message={state.error}
          style={{ marginBottom: 16, borderRadius: 8 }} />
      )}

      {/* KPIs */}
      {state.loading ? (
        <div style={styles.kpiGrid}>
          {[0,1,2,3,4].map(i => <div key={i} style={styles.skel}><Skeleton active paragraph={{ rows: 2 }} title={false} /></div>)}
        </div>
      ) : (
        <div style={styles.kpiGrid}>
          <KpiCard label="Open claims" value={state.openClaims.toLocaleString()} />
          <KpiCard label="Avg TAT" value={state.avgTat !== null ? `${state.avgTat.toFixed(1)}d` : '—'}
            delta={tatVsSla !== null ? `${tatVsSla >= 0 ? '+' : ''}${tatVsSla.toFixed(1)}d vs SLA` : undefined}
            deltaDirection={tatVsSla !== null ? (tatVsSla > 0 ? 'up' : 'down') : undefined}
            deltaTone={tatVsSla !== null && tatVsSla > 0 ? 'down' : 'up'} />
          <KpiCard label="SLA violations" value={state.slaViolations.toLocaleString()}
            tone={state.slaViolations > 0 ? 'warning' : 'default'}
            delta={state.slaViolations > 0 ? 'Action needed' : 'Clear'}
            deltaTone={state.slaViolations > 0 ? 'down' : 'up'} />
          <KpiCard label="Avg cycle time" value={state.avgCycle !== null ? `${state.avgCycle.toFixed(1)}d` : '—'} />
          <KpiCard label="Team volume" value={state.teamVolume.toLocaleString()}
            delta="claims handled" deltaDirection="flat" />
        </div>
      )}

      {/* TAT trend with SLA reference line */}
      <div style={{ marginTop: 12, marginBottom: 12 }}>
        <DataCard title="Avg TAT trend" subtitle={`Last 8 weeks · dashed line = ${SLA_TAT_DAYS}-day SLA target`}>
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.weeklyTat.every(p => p.tat === 0) ? (
            <EmptyState title="No TAT data" description="Trend will appear once KPIs are computed." tone="positive" />
          ) : (
            <div style={styles.trendWrap}>
              <div style={styles.trendBars}>
                {state.weeklyTat.map(p => {
                  const max = Math.max(...state.weeklyTat.map(x => x.tat), SLA_TAT_DAYS, 1);
                  const h = (p.tat / max) * 100;
                  const breached = p.tat > SLA_TAT_DAYS;
                  return (
                    <div key={p.week} style={styles.trendCol}>
                      <div style={styles.trendBarTrack}>
                        <div style={{ ...styles.trendBarFill, height: `${h}%`,
                                      background: breached ? CHART.red : CHART.blue }} />
                      </div>
                      <div style={styles.trendLabel}>{p.week}</div>
                      <div style={styles.trendCount}>{p.tat}d</div>
                    </div>
                  );
                })}
              </div>
              {/* Dashed SLA line — positioned at the SLA height */}
              <div style={{
                ...styles.slaLine,
                bottom: `${24 + (SLA_TAT_DAYS / Math.max(...state.weeklyTat.map(x => x.tat), SLA_TAT_DAYS, 1)) * 100}px`,
              }}>
                <span style={styles.slaLineLabel}>SLA · {SLA_TAT_DAYS}d</span>
              </div>
            </div>
          )}
        </DataCard>
      </div>

      {/* Adjuster workload table */}
      <DataCard title="Adjuster workload" subtitle="Most recent quarter · click # to drill in" padding={0}>
        {state.loading ? (
          <div style={{ padding: 16 }}><Skeleton active paragraph={{ rows: 5 }} title={false} /></div>
        ) : state.adjusters.length === 0 ? (
          <EmptyState title="No adjuster data" description="Performance records will appear as quarters close." />
        ) : (
          <Table rowKey="perfId" columns={adjusterCols} dataSource={state.adjusters}
            size="small" pagination={false} />
        )}
      </DataCard>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiGrid: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 12 },
  skel:    { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 10, padding: '16px 20px', minHeight: 88 },
  trendWrap:{ position: 'relative' },
  trendBars:{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 140, padding: '8px 0' },
  trendCol: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 },
  trendBarTrack: { width: '100%', height: 100, background: 'var(--ci-bg-surface-2)', borderRadius: 4, display: 'flex', alignItems: 'flex-end', overflow: 'hidden' },
  trendBarFill:  { width: '100%', borderRadius: 4, transition: 'height 0.3s' },
  trendLabel:{ fontSize: 10, color: 'var(--ci-text-muted)' },
  trendCount:{ fontSize: 11, fontWeight: 500, color: 'var(--ci-text-secondary)' },
  slaLine:  { position: 'absolute', left: 0, right: 0, borderTop: '1.5px dashed var(--ci-danger-text, #B22C2B)', pointerEvents: 'none' },
  slaLineLabel:{ position: 'absolute', right: 0, top: -16, fontSize: 9, color: 'var(--ci-danger-text, #B22C2B)', background: 'var(--ci-bg-surface)', padding: '0 4px', fontWeight: 500 },
};

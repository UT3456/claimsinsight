import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Alert, Skeleton } from 'antd';
import { RefreshCw } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import { useAuth } from '../../../hooks/useAuth';

import { ingestApi }                      from '../../../api/dataIngestionApi';
import { claimsApi }                      from '../../../api/claimsApi';
import { denialPatternsApi }              from '../../../api/denialLeakageApi';
import { costsApi }                       from '../../../api/financialApi';
import { settledArray }                   from '../../../utils/settled';

/**
 * AnalystDashboard — first-screen view for ROLE_CLAIMS_ANALYST.
 *
 * Per SRS Module 4.5 (denial & leakage) + 4.7 (cost drivers): an analyst's
 * job is to spot trends in denials and where money is going. So the page
 * leads with denial rate + cost driver breakdown, not generic claim totals.
 *
 * KPIs:    Total claims · Denial rate % · Avg severity · Loss ratio · Frequency
 * Panels:  Denial trend (last 8 weeks) · Denial reasons bar · Cost driver
 *          breakdown by CostType (Medical / Legal / Repair / Settlement) ·
 *          Severity by claim type
 */

interface State {
  loading: boolean;
  error: string | null;
  totalClaims: number;
  denialRate: number;       // %
  avgSeverity: number;      // $
  lossRatio: number | null; // %
  frequency: number | null;
  denialTrend: { week: string; count: number }[];
  denialReasons: { reason: string; count: number; pct: number }[];
  costByType: { type: string; total: number; pct: number }[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  totalClaims: 0, denialRate: 0, avgSeverity: 0, lossRatio: null, frequency: null,
  denialTrend: [], denialReasons: [], costByType: [],
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START': return { ...s, loading: true, error: null };
    case 'OK':    return { ...s, loading: false, ...a.payload };
    case 'ERR':   return { ...s, loading: false, error: a.payload };
    default:      return s;
  }
}

function weekKey(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const start = new Date(d); start.setDate(d.getDate() - d.getDay());
  return `${start.getMonth() + 1}/${start.getDate()}`;
}

function mean(nums: number[]): number | null {
  if (nums.length === 0) return null;
  return nums.reduce((a, b) => a + b, 0) / nums.length;
}

export default function AnalystDashboard() {
  const { username } = useAuth();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [rawR, sevR, lossR, freqR, denialR, costR] = await Promise.allSettled([
        ingestApi.getAll(),
        claimsApi.getByMetric('SEVERITY'),
        claimsApi.getByMetric('LOSS_RATIO'),
        claimsApi.getByMetric('FREQUENCY'),
        denialPatternsApi.getAll(),
        costsApi.getAll(),
      ]);
      const raw      = settledArray<{ ingestedDate: string }>(rawR);
      const sevs     = settledArray<{ metricValue: number }>(sevR);
      const losses   = settledArray<{ metricValue: number }>(lossR);
      const freqs    = settledArray<{ metricValue: number }>(freqR);
      const denials  = settledArray<{ reason: string; occurrenceDate: string }>(denialR);
      const costs    = settledArray<{ costType: string; amount: number }>(costR);

      // ── Denial trend over last 8 weeks ─────────────────────────────────
      const bins = new Map<string, number>();
      const now = new Date();
      for (let i = 7; i >= 0; i--) {
        const d = new Date(now); d.setDate(d.getDate() - i * 7);
        bins.set(weekKey(d.toISOString()), 0);
      }
      for (const d of denials) {
        const key = weekKey(d.occurrenceDate);
        if (bins.has(key)) bins.set(key, (bins.get(key) ?? 0) + 1);
      }
      const denialTrend = Array.from(bins.entries()).map(([week, count]) => ({ week, count }));

      // ── Denial reasons (top 5) ─────────────────────────────────────────
      const tally = new Map<string, number>();
      for (const d of denials) tally.set(d.reason, (tally.get(d.reason) ?? 0) + 1);
      const total = denials.length || 1;
      const denialReasons = Array.from(tally.entries())
        .map(([reason, count]) => ({ reason, count, pct: Math.round((count / total) * 100) }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5);

      // ── Cost breakdown by type ─────────────────────────────────────────
      const costSums = new Map<string, number>();
      for (const c of costs) costSums.set(c.costType, (costSums.get(c.costType) ?? 0) + c.amount);
      const totalCost = Array.from(costSums.values()).reduce((a, b) => a + b, 0) || 1;
      const costByType = Array.from(costSums.entries())
        .map(([type, total]) => ({ type, total, pct: Math.round((total / totalCost) * 100) }))
        .sort((a, b) => b.total - a.total);

      dispatch({
        type: 'OK',
        payload: {
          totalClaims:   raw.length,
          denialRate:    raw.length > 0 ? Math.round((denials.length / raw.length) * 100) : 0,
          avgSeverity:   mean(sevs.map(s => s.metricValue)) ?? 0,
          lossRatio:     mean(losses.map(l => l.metricValue)),
          frequency:     mean(freqs.map(f => f.metricValue)),
          denialTrend,
          denialReasons,
          costByType,
        },
      });
    } catch (err) {
      dispatch({ type: 'ERR', payload: (err as { userMessage?: string }).userMessage ?? 'Failed to load dashboard.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const firstName = useMemo(() => (username ?? 'Analyst').split(/[\s.]/)[0], [username]);

  const fmtMoney = (n: number) => n >= 1000 ? `$${(n / 1000).toFixed(1)}K` : `$${Math.round(n)}`;

  return (
    <div>
      <PageHeader
        title="Claims overview"
        subtitle={`Welcome back, ${firstName} · denial trends & cost drivers`}
        actions={<DarkButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>Refresh</DarkButton>}
      />

      {state.error && (
        <Alert type="warning" showIcon closable message={state.error}
          description="Some services may be offline. Showing available data."
          style={{ marginBottom: 16, borderRadius: 8 }} />
      )}

      {/* KPI row */}
      {state.loading ? (
        <div style={styles.kpiGrid}>
          {[0, 1, 2, 3, 4].map(i => <div key={i} style={styles.skel}><Skeleton active paragraph={{ rows: 2 }} title={false} /></div>)}
        </div>
      ) : (
        <div style={styles.kpiGrid}>
          <KpiCard label="Total claims"  value={state.totalClaims.toLocaleString()} />
          <KpiCard label="Denial rate"   value={`${state.denialRate}%`}
                   delta={state.denialRate >= 12 ? 'Above benchmark' : 'Within range'}
                   deltaTone={state.denialRate >= 12 ? 'down' : 'up'} />
          <KpiCard label="Avg severity"  value={fmtMoney(state.avgSeverity)} />
          <KpiCard label="Loss ratio"    value={state.lossRatio !== null ? `${state.lossRatio.toFixed(1)}%` : '—'} />
          <KpiCard label="Frequency"     value={state.frequency !== null ? state.frequency.toFixed(2) : '—'}
                   delta="claims/policy-yr" deltaDirection="flat" />
        </div>
      )}

      {/* Row 1 — denial trend + cost driver breakdown */}
      <div style={styles.twoCol}>
        <DataCard title="Denial trend" subtitle="Last 8 weeks">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.denialTrend.every(p => p.count === 0) ? (
            <EmptyState title="No denials" description="No denial patterns recorded in the last 8 weeks." tone="positive" />
          ) : (
            <div style={styles.trendBars}>
              {state.denialTrend.map(p => {
                const max = Math.max(...state.denialTrend.map(x => x.count), 1);
                const h = (p.count / max) * 100;
                return (
                  <div key={p.week} style={styles.trendCol}>
                    <div style={styles.trendBarTrack}>
                      <div style={{ ...styles.trendBarFill, height: `${h}%`, background: CHART.amber }} />
                    </div>
                    <div style={styles.trendLabel}>{p.week}</div>
                    <div style={styles.trendCount}>{p.count}</div>
                  </div>
                );
              })}
            </div>
          )}
        </DataCard>

        <DataCard title="Cost drivers" subtitle="Spend by category">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.costByType.length === 0 ? (
            <EmptyState title="No cost records" description="Costs will appear once claims are paid." />
          ) : (
            state.costByType.map((c, i) => {
              const max = Math.max(...state.costByType.map(x => x.total), 1);
              const pct = (c.total / max) * 100;
              const colors = [CHART.blue, CHART.purple, CHART.teal, CHART.amber];
              return (
                <div key={c.type} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span style={{ textTransform: 'capitalize' }}>{c.type.toLowerCase()}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>
                      {fmtMoney(c.total)} · {c.pct}%
                    </span>
                  </div>
                  <div style={styles.barTrack}>
                    <div style={{ ...styles.barFill, width: `${pct}%`, background: colors[i % colors.length] }} />
                  </div>
                </div>
              );
            })
          )}
        </DataCard>
      </div>

      {/* Row 2 — denial reasons */}
      <div style={{ marginTop: 12 }}>
        <DataCard title="Top denial reasons" subtitle="By occurrence">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 5 }} title={false} />
          ) : state.denialReasons.length === 0 ? (
            <EmptyState title="No denials logged" description="Denial patterns will surface here as they're detected." tone="positive" />
          ) : (
            state.denialReasons.map((d, i) => {
              const max = Math.max(...state.denialReasons.map(x => x.count), 1);
              const pct = (d.count / max) * 100;
              const color = i < 2 ? CHART.purple : '#AFA9EC';
              return (
                <div key={d.reason} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span>{d.reason}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>{d.count} · {d.pct}%</span>
                  </div>
                  <div style={styles.barTrack}>
                    <div style={{ ...styles.barFill, width: `${pct}%`, background: color }} />
                  </div>
                </div>
              );
            })
          )}
        </DataCard>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiGrid: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 12 },
  skel:    { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 10, padding: '16px 20px', minHeight: 88 },
  twoCol:  { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 },
  barRow:  { marginBottom: 10 },
  barHead: { display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 4 },
  barTrack:{ height: 8, background: 'var(--ci-bg-surface-2)', borderRadius: 4, overflow: 'hidden' },
  barFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
  trendBars:{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 140, padding: '8px 0' },
  trendCol: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 },
  trendBarTrack: { width: '100%', height: 100, background: 'var(--ci-bg-surface-2)', borderRadius: 4, display: 'flex', alignItems: 'flex-end', overflow: 'hidden' },
  trendBarFill:  { width: '100%', borderRadius: 4, transition: 'height 0.3s' },
  trendLabel:{ fontSize: 10, color: 'var(--ci-text-muted)' },
  trendCount:{ fontSize: 11, fontWeight: 500, color: 'var(--ci-text-secondary)' },
};

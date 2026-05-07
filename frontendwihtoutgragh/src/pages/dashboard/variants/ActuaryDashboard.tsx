import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Alert, Skeleton, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefreshCw } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import type { BadgeTone } from '../../../components/ui';
import { useAuth } from '../../../hooks/useAuth';

import { claimsApi } from '../../../api/claimsApi';
import { reservesApi, costsApi, agingApi } from '../../../api/financialApi';
import { settledArray } from '../../../utils/settled';

/**
 * ActuaryDashboard — first-screen view for ROLE_ACTUARY.
 *
 * Per SRS Module 4.7 (cost & trends): an actuary cares about severity,
 * frequency, reserves, and whether reserves are adequate against actual
 * paid losses. Loss-development triangle is the canonical actuarial chart;
 * we ship a simplified version (severity by month) here — a true triangle
 * needs accident-year cohorts joined across cost+reserve which is a much
 * larger piece of work.
 *
 * KPIs:    Avg severity · Avg frequency · Total reserves · Loss ratio · Reserve adequacy
 * Panels:  Severity trend (last 6 months) · Reserve adequacy table
 *          (reserve vs paid per claim) · Aging analysis
 */

interface AdequacyRow {
  claimId: string;
  reserve: number;
  paid: number;
  ratio: number;     // paid/reserve %
  flag: 'OVER' | 'UNDER' | 'OK';
}

interface State {
  loading: boolean;
  error: string | null;
  avgSeverity: number;
  avgFrequency: number | null;
  totalReserves: number;
  lossRatio: number | null;
  reserveAdequacy: number;     // % of claims where reserve covers paid
  severityTrend: { month: string; severity: number }[];
  adequacyRows: AdequacyRow[];
  agingTotals: { label: string; count: number; color: string }[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  avgSeverity: 0, avgFrequency: null, totalReserves: 0, lossRatio: null,
  reserveAdequacy: 0,
  severityTrend: [], adequacyRows: [], agingTotals: [],
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

function monthKey(iso: string): string {
  const d = new Date(iso);
  return isNaN(d.getTime()) ? '' : d.toLocaleDateString(undefined, { month: 'short' });
}

function fmtMoney(n: number): string {
  if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(2)}M`;
  if (n >= 1_000)     return `$${(n / 1_000).toFixed(1)}K`;
  return `$${Math.round(n)}`;
}

export default function ActuaryDashboard() {
  const { username } = useAuth();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [sevR, freqR, lossR, resR, costR, agingR] = await Promise.allSettled([
        claimsApi.getByMetric('SEVERITY'),
        claimsApi.getByMetric('FREQUENCY'),
        claimsApi.getByMetric('LOSS_RATIO'),
        reservesApi.getAll(),
        costsApi.getAll(),
        agingApi.getDistribution(),
      ]);
      // Defensive: settledArray() coerces non-array responses to [] so
      // `for (const x of …)` never throws "not iterable".
      const sevs     = settledArray<{ metricDate: string; metricValue: number }>(sevR);
      const freqs    = settledArray<{ metricValue: number }>(freqR);
      const losses   = settledArray<{ metricValue: number }>(lossR);
      const reserves = settledArray<{ claimId: string; reserveAmount: number }>(resR);
      const costs    = settledArray<{ claimId: string; amount: number }>(costR);
      const aging    = settledArray<{ agingBucket: string; count: number }>(agingR);

      // ── Severity trend (last 6 months) ─────────────────────────────────
      const months: { key: string; vals: number[] }[] = [];
      const now = new Date();
      for (let i = 5; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        months.push({ key: monthKey(d.toISOString()), vals: [] });
      }
      const labels = months.map(m => m.key);
      for (const s of sevs) {
        const k = monthKey(s.metricDate);
        const idx = labels.indexOf(k);
        if (idx >= 0) months[idx].vals.push(s.metricValue);
      }
      const severityTrend = months.map(m => ({
        month: m.key,
        severity: m.vals.length ? Math.round(m.vals.reduce((a, b) => a + b, 0) / m.vals.length) : 0,
      }));

      // ── Reserve adequacy: reserve vs paid (sum of cost rows) per claim ──
      const paidByClaim = new Map<string, number>();
      for (const c of costs) {
        paidByClaim.set(c.claimId, (paidByClaim.get(c.claimId) ?? 0) + c.amount);
      }
      const adequacyRows: AdequacyRow[] = reserves.map(r => {
        const paid = paidByClaim.get(r.claimId) ?? 0;
        const ratio = r.reserveAmount > 0 ? (paid / r.reserveAmount) * 100 : 0;
        const flag: AdequacyRow['flag'] =
            ratio > 100 ? 'OVER'
          : ratio < 60  ? 'UNDER'
          :               'OK';
        return { claimId: r.claimId, reserve: r.reserveAmount, paid, ratio, flag };
      });
      // Show the most concerning rows first (OVER, then UNDER, then OK)
      adequacyRows.sort((a, b) => {
        const pri = (f: AdequacyRow['flag']) => f === 'OVER' ? 0 : f === 'UNDER' ? 1 : 2;
        return pri(a.flag) - pri(b.flag);
      });
      const top = adequacyRows.slice(0, 8);
      const adequateCount = adequacyRows.filter(r => r.flag === 'OK').length;
      const reserveAdequacy = adequacyRows.length
        ? Math.round((adequateCount / adequacyRows.length) * 100)
        : 0;

      // ── Aging totals (already computed by service) ──────────────────────
      const bucketCount = (b: string) => aging.find(x => x.agingBucket === b)?.count ?? 0;
      const agingTotals = [
        { label: '0–30 d',  count: bucketCount('BUCKET_0_30'),    color: CHART.teal  },
        { label: '31–60 d', count: bucketCount('BUCKET_31_60'),   color: CHART.blue  },
        { label: '61–90 d', count: bucketCount('BUCKET_61_90'),   color: CHART.amber },
        { label: '90+ d',   count: bucketCount('BUCKET_90_PLUS'), color: CHART.red   },
      ];

      dispatch({
        type: 'OK',
        payload: {
          avgSeverity:   mean(sevs.map(s => s.metricValue)) ?? 0,
          avgFrequency:  mean(freqs.map(f => f.metricValue)),
          totalReserves: reserves.reduce((s, r) => s + r.reserveAmount, 0),
          lossRatio:     mean(losses.map(l => l.metricValue)),
          reserveAdequacy,
          severityTrend,
          adequacyRows: top,
          agingTotals,
        },
      });
    } catch (err) {
      // Surface the real error so we can debug, not just a blanket toast.
      // The Actuary dashboard fans out to 6 services; any one of them
      // returning a malformed shape used to trigger a top-level catch with
      // no clue as to which call broke. Now the stack lands in DevTools.
      // eslint-disable-next-line no-console
      console.error('[ActuaryDashboard] load failed:', err);
      const msg = (err as { userMessage?: string }).userMessage
                ?? (err instanceof Error ? err.message : 'Failed to load dashboard.');
      dispatch({ type: 'ERR', payload: msg });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const firstName = useMemo(() => (username ?? 'Actuary').split(/[\s.]/)[0], [username]);

  const flagTone: Record<AdequacyRow['flag'], BadgeTone> = {
    OK:    'green',
    UNDER: 'amber',
    OVER:  'red',
  };
  const flagLabel: Record<AdequacyRow['flag'], string> = {
    OK:    'Adequate',
    UNDER: 'Excess reserve',
    OVER:  'Reserve breached',
  };

  const adequacyCols: ColumnsType<AdequacyRow> = [
    {
      title: 'Claim ID', dataIndex: 'claimId', key: 'claimId', width: 200,
      render: v => <span style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 11 }}>{v}</span>,
    },
    {
      title: 'Reserve', dataIndex: 'reserve', key: 'reserve', align: 'right', width: 110,
      render: v => fmtMoney(v),
    },
    {
      title: 'Paid', dataIndex: 'paid', key: 'paid', align: 'right', width: 110,
      render: v => fmtMoney(v),
    },
    {
      title: 'Paid / Reserve', dataIndex: 'ratio', key: 'ratio', align: 'right', width: 130,
      render: (v: number) => `${v.toFixed(0)}%`,
    },
    {
      title: 'Status', dataIndex: 'flag', key: 'flag', width: 160,
      render: (v: AdequacyRow['flag']) => <Badge tone={flagTone[v]}>{flagLabel[v]}</Badge>,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Actuarial overview"
        subtitle={`${firstName} · severity · reserves · adequacy`}
        actions={<DarkButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>Refresh</DarkButton>}
      />

      {state.error && (
        <Alert type="warning" showIcon closable message={state.error}
          style={{ marginBottom: 16, borderRadius: 8 }} />
      )}

      {state.loading ? (
        <div style={styles.kpiGrid}>
          {[0,1,2,3,4].map(i => <div key={i} style={styles.skel}><Skeleton active paragraph={{ rows: 2 }} title={false} /></div>)}
        </div>
      ) : (
        <div style={styles.kpiGrid}>
          <KpiCard label="Avg severity" value={fmtMoney(state.avgSeverity)} />
          <KpiCard label="Avg frequency"
            value={state.avgFrequency !== null ? state.avgFrequency.toFixed(2) : '—'}
            delta="claims/policy-yr" deltaDirection="flat" />
          <KpiCard label="Total reserves" value={fmtMoney(state.totalReserves)} />
          <KpiCard label="Loss ratio"
            value={state.lossRatio !== null ? `${state.lossRatio.toFixed(1)}%` : '—'} />
          <KpiCard label="Reserve adequacy"
            value={`${state.reserveAdequacy}%`}
            tone={state.reserveAdequacy < 80 ? 'warning' : 'default'}
            delta={state.reserveAdequacy >= 90 ? 'Healthy' : state.reserveAdequacy >= 80 ? 'Watch' : 'Action'}
            deltaTone={state.reserveAdequacy >= 90 ? 'up' : 'down'} />
        </div>
      )}

      {/* Severity trend + aging */}
      <div style={styles.twoCol}>
        <DataCard title="Severity trend" subtitle="Avg severity by month · last 6 mo">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.severityTrend.every(p => p.severity === 0) ? (
            <EmptyState title="No severity data" description="Trend will appear once KPIs are computed." />
          ) : (
            <div style={styles.trendBars}>
              {state.severityTrend.map(p => {
                const max = Math.max(...state.severityTrend.map(x => x.severity), 1);
                const h = (p.severity / max) * 100;
                return (
                  <div key={p.month} style={styles.trendCol}>
                    <div style={styles.trendBarTrack}>
                      <div style={{ ...styles.trendBarFill, height: `${h}%`, background: CHART.purple }} />
                    </div>
                    <div style={styles.trendLabel}>{p.month}</div>
                    <div style={styles.trendCount}>{fmtMoney(p.severity)}</div>
                  </div>
                );
              })}
            </div>
          )}
        </DataCard>

        <DataCard title="Aging — reserve exposure" subtitle="Days since filed">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.agingTotals.every(a => a.count === 0) ? (
            <EmptyState title="No aging data" description="Buckets populate as claims age." tone="positive" />
          ) : (
            state.agingTotals.map(a => {
              const max = Math.max(...state.agingTotals.map(x => x.count), 1);
              const pct = (a.count / max) * 100;
              return (
                <div key={a.label} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span>{a.label}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>{a.count.toLocaleString()}</span>
                  </div>
                  <div style={styles.barTrack}>
                    <div style={{ ...styles.barFill, width: `${pct}%`, background: a.color }} />
                  </div>
                </div>
              );
            })
          )}
        </DataCard>
      </div>

      {/* Reserve adequacy table — most concerning first */}
      <div style={{ marginTop: 12 }}>
        <DataCard
          title="Reserve adequacy"
          subtitle="Reserved vs paid · sorted by exposure (breaches first)"
          padding={0}
        >
          {state.loading ? (
            <div style={{ padding: 16 }}><Skeleton active paragraph={{ rows: 5 }} title={false} /></div>
          ) : state.adequacyRows.length === 0 ? (
            <EmptyState title="No reserves" description="Reserves appear once claims are seeded." />
          ) : (
            <Table rowKey="claimId" columns={adequacyCols} dataSource={state.adequacyRows}
              size="small" pagination={false} />
          )}
        </DataCard>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiGrid: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 12 },
  skel:    { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 10, padding: '16px 20px', minHeight: 88 },
  twoCol:  { display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 12 },
  barRow:  { marginBottom: 10 },
  barHead: { display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 4 },
  barTrack:{ height: 8, background: 'var(--ci-bg-surface-2)', borderRadius: 4, overflow: 'hidden' },
  barFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
  trendBars:{ display: 'flex', alignItems: 'flex-end', gap: 12, height: 160, padding: '8px 0' },
  trendCol: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 },
  trendBarTrack: { width: '100%', height: 120, background: 'var(--ci-bg-surface-2)', borderRadius: 4, display: 'flex', alignItems: 'flex-end', overflow: 'hidden' },
  trendBarFill:  { width: '100%', borderRadius: 4, transition: 'height 0.3s' },
  trendLabel:{ fontSize: 10, color: 'var(--ci-text-muted)' },
  trendCount:{ fontSize: 10, fontWeight: 500, color: 'var(--ci-text-secondary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' },
};

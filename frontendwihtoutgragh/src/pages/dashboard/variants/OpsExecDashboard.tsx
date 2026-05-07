import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Alert, Skeleton } from 'antd';
import { RefreshCw } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import { useAuth } from '../../../hooks/useAuth';

import { ingestApi }            from '../../../api/dataIngestionApi';
import { claimsApi }            from '../../../api/claimsApi';
import { adjustersApi, slaApi } from '../../../api/adjustersApi';
import { agingApi }             from '../../../api/financialApi';
import { settledArray }         from '../../../utils/settled';

/**
 * OpsExecDashboard — first-screen view for ROLE_OPERATIONS_EXEC.
 *
 * Per SRS Module 4.3 + 4.6 + 4.8: an exec wants the portfolio at a glance —
 * not individual claim rows. KPIs are ratios and percentages; charts are
 * month-over-month trends; everything is rolled up.
 *
 * KPIs:    Loss ratio · Combined ratio (loss + expense ratio est.) ·
 *          Settlement rate · SLA compliance % · Open claims portfolio
 * Panels:  Month-over-month loss-ratio trend · SLA compliance gauge ·
 *          Aging bucket portfolio summary
 */

interface State {
  loading: boolean;
  error: string | null;
  lossRatio: number | null;
  combinedRatio: number | null;
  settlementRate: number | null;
  slaCompliance: number | null;
  openClaims: number;
  monthlyTrend: { month: string; lossRatio: number; tat: number }[];
  agingTotals: { label: string; count: number; tone: 'good' | 'ok' | 'warn' | 'danger' }[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  lossRatio: null, combinedRatio: null, settlementRate: null,
  slaCompliance: null, openClaims: 0,
  monthlyTrend: [], agingTotals: [],
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
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString(undefined, { month: 'short' });
}

export default function OpsExecDashboard() {
  const { username } = useAuth();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [rawR, lossR, tatR, settleR, adjR, slaR, agingR] = await Promise.allSettled([
        ingestApi.getAll(),
        claimsApi.getByMetric('LOSS_RATIO'),
        claimsApi.getByMetric('TAT'),
        claimsApi.getByMetric('SETTLEMENT_TIME'),
        adjustersApi.getAll(),
        slaApi.getAll(),
        agingApi.getDistribution(),
      ]);
      const raw       = settledArray<unknown>(rawR);
      const losses    = settledArray<{ metricValue: number; metricDate: string }>(lossR);
      const tats      = settledArray<{ metricValue: number; metricDate: string }>(tatR);
      const settles   = settledArray<unknown>(settleR);
      const adjusters = settledArray<{ slaMetCount?: number; slaBreachedCount?: number }>(adjR);
      const slas      = settledArray<unknown>(slaR);
      const aging     = settledArray<{ agingBucket: string; count: number }>(agingR);

      // ── KPIs ───────────────────────────────────────────────────────────
      const lossRatio = mean(losses.map(l => l.metricValue));
      // Combined ratio ~= loss ratio + ~28 expense ratio (industry rough avg).
      // Without actual expense data we use a flat 28% adder so the KPI is
      // directionally meaningful rather than fake-precise.
      const combinedRatio = lossRatio !== null ? lossRatio + 28 : null;

      // Settlement rate = % of claims with SETTLEMENT_TIME computed. Crude but
      // gives a reasonable proxy without a closed-status flag in claim_raw.
      const settlementRate = raw.length > 0
        ? Math.min(100, (settles.length / raw.length) * 100)
        : null;

      // SLA compliance = sla_met / (sla_met + sla_breached) across adjusters.
      const slaMet      = adjusters.reduce((s, a) => s + (a.slaMetCount ?? 0), 0);
      const slaBreached = adjusters.reduce((s, a) => s + (a.slaBreachedCount ?? 0), 0);
      const slaCompliance = (slaMet + slaBreached) > 0
        ? (slaMet / (slaMet + slaBreached)) * 100
        : null;

      // ── MoM trend (last 6 months) ──────────────────────────────────────
      const months: { key: string; losses: number[]; tats: number[] }[] = [];
      const now = new Date();
      for (let i = 5; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        months.push({ key: monthKey(d.toISOString()), losses: [], tats: [] });
      }
      const labels = months.map(m => m.key);
      for (const l of losses) {
        const k = monthKey(l.metricDate);
        const idx = labels.indexOf(k);
        if (idx >= 0) months[idx].losses.push(l.metricValue);
      }
      for (const t of tats) {
        const k = monthKey(t.metricDate);
        const idx = labels.indexOf(k);
        if (idx >= 0) months[idx].tats.push(t.metricValue);
      }
      const monthlyTrend = months.map(m => ({
        month:     m.key,
        lossRatio: m.losses.length > 0 ? Math.round(m.losses.reduce((a, b) => a + b, 0) / m.losses.length) : 0,
        tat:       m.tats.length   > 0 ? Math.round(m.tats.reduce((a, b) => a + b, 0) / m.tats.length)     : 0,
      }));

      // ── Aging totals (API returns Array<{agingBucket, count}>) ─────────
      const bucketCount = (b: string) =>
        aging.find(x => x.agingBucket === b)?.count ?? 0;
      const agingTotals: State['agingTotals'] = [
        { label: '0–30 d',  count: bucketCount('BUCKET_0_30'),    tone: 'good'   },
        { label: '31–60 d', count: bucketCount('BUCKET_31_60'),   tone: 'ok'     },
        { label: '61–90 d', count: bucketCount('BUCKET_61_90'),   tone: 'warn'   },
        { label: '90+ d',   count: bucketCount('BUCKET_90_PLUS'), tone: 'danger' },
      ];

      void slas; // SLA list not displayed at portfolio level — count rolled into compliance %

      dispatch({
        type: 'OK',
        payload: {
          lossRatio, combinedRatio, settlementRate, slaCompliance,
          openClaims: raw.length,
          monthlyTrend, agingTotals,
        },
      });
    } catch (err) {
      dispatch({ type: 'ERR', payload: (err as { userMessage?: string }).userMessage ?? 'Failed to load dashboard.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const firstName = useMemo(() => (username ?? 'Executive').split(/[\s.]/)[0], [username]);

  // SLA gauge fill — percentage as a horizontal bar
  const slaFill = state.slaCompliance ?? 0;
  const slaTone = slaFill >= 90 ? CHART.teal : slaFill >= 75 ? CHART.amber : CHART.red;

  return (
    <div>
      <PageHeader
        title="Portfolio overview"
        subtitle={`${firstName} · executive view · all metrics rolled up`}
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
          <KpiCard label="Loss ratio"
            value={state.lossRatio !== null ? `${state.lossRatio.toFixed(1)}%` : '—'}
            tone={(state.lossRatio ?? 0) > 75 ? 'warning' : 'default'}
            delta={(state.lossRatio ?? 0) > 75 ? 'Above target' : 'Healthy'}
            deltaTone={(state.lossRatio ?? 0) > 75 ? 'down' : 'up'} />
          <KpiCard label="Combined ratio"
            value={state.combinedRatio !== null ? `${state.combinedRatio.toFixed(1)}%` : '—'}
            delta="loss + expense est." deltaDirection="flat" />
          <KpiCard label="Settlement rate"
            value={state.settlementRate !== null ? `${state.settlementRate.toFixed(0)}%` : '—'} />
          <KpiCard label="SLA compliance"
            value={state.slaCompliance !== null ? `${state.slaCompliance.toFixed(0)}%` : '—'}
            tone={(state.slaCompliance ?? 100) < 85 ? 'warning' : 'default'} />
          <KpiCard label="Open portfolio" value={state.openClaims.toLocaleString()}
            delta="claims" deltaDirection="flat" />
        </div>
      )}

      {/* MoM trend + SLA gauge */}
      <div style={styles.twoCol}>
        <DataCard title="Month-over-month" subtitle="Loss ratio % (bars) · Avg TAT in days (line)">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.monthlyTrend.every(m => m.lossRatio === 0 && m.tat === 0) ? (
            <EmptyState title="No monthly data" description="Trend will appear as KPIs are computed." />
          ) : (
            <div style={styles.trendBars}>
              {state.monthlyTrend.map(m => {
                const max = Math.max(...state.monthlyTrend.map(x => x.lossRatio), 100);
                const h = (m.lossRatio / max) * 100;
                const tatScale = Math.max(...state.monthlyTrend.map(x => x.tat), 30);
                const tatH = (m.tat / tatScale) * 100;
                return (
                  <div key={m.month} style={styles.trendCol}>
                    <div style={styles.trendBarTrack}>
                      <div style={{ ...styles.trendBarFill, height: `${h}%`, background: CHART.blue }} />
                      <div style={{ ...styles.trendDot, bottom: `${tatH}%` }} title={`TAT: ${m.tat}d`} />
                    </div>
                    <div style={styles.trendLabel}>{m.month}</div>
                    <div style={styles.trendCount}>{m.lossRatio}%</div>
                  </div>
                );
              })}
            </div>
          )}
        </DataCard>

        <DataCard title="SLA compliance" subtitle="% of claims meeting regulatory TAT">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 3 }} title={false} />
          ) : state.slaCompliance === null ? (
            <EmptyState title="No SLA data" description="Compliance will surface as adjuster periods close." />
          ) : (
            <div style={styles.gaugeWrap}>
              <div style={styles.gaugeNumber}>{slaFill.toFixed(0)}%</div>
              <div style={styles.gaugeTrack}>
                <div style={{ ...styles.gaugeFill, width: `${Math.min(slaFill, 100)}%`, background: slaTone }} />
              </div>
              <div style={styles.gaugeMarks}>
                <span>0%</span>
                <span>50%</span>
                <span style={{ color: 'var(--ci-text-muted)' }}>Target&nbsp;90%</span>
                <span>100%</span>
              </div>
              <div style={styles.gaugeNote}>
                {slaFill >= 90 ? 'Meeting target — portfolio healthy.'
                 : slaFill >= 75 ? 'Below target — review at-risk teams.'
                 : 'Critical — urgent intervention required.'}
              </div>
            </div>
          )}
        </DataCard>
      </div>

      {/* Aging summary — portfolio level */}
      <div style={{ marginTop: 12 }}>
        <DataCard title="Aging — portfolio summary" subtitle="Days since filed">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.agingTotals.every(a => a.count === 0) ? (
            <EmptyState title="No aging data" description="Buckets populate as claims age." tone="positive" />
          ) : (
            <div style={styles.agingGrid}>
              {state.agingTotals.map(a => {
                const color = a.tone === 'good'   ? CHART.teal
                            : a.tone === 'ok'     ? CHART.blue
                            : a.tone === 'warn'   ? CHART.amber : CHART.red;
                return (
                  <div key={a.label} style={styles.agingCell}>
                    <div style={styles.agingLabel}>{a.label}</div>
                    <div style={{ ...styles.agingValue, color }}>{a.count.toLocaleString()}</div>
                  </div>
                );
              })}
            </div>
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
  trendBars:{ display: 'flex', alignItems: 'flex-end', gap: 12, height: 160, padding: '8px 0' },
  trendCol: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 },
  trendBarTrack: { width: '100%', height: 120, background: 'var(--ci-bg-surface-2)', borderRadius: 4, position: 'relative', display: 'flex', alignItems: 'flex-end', overflow: 'visible' },
  trendBarFill:  { width: '100%', borderRadius: 4, transition: 'height 0.3s' },
  trendDot:      { position: 'absolute', left: '50%', width: 8, height: 8, marginLeft: -4, borderRadius: '50%', background: '#C77800', border: '2px solid var(--ci-bg-surface)', transition: 'bottom 0.3s' },
  trendLabel:{ fontSize: 10, color: 'var(--ci-text-muted)' },
  trendCount:{ fontSize: 11, fontWeight: 500, color: 'var(--ci-text-secondary)' },
  gaugeWrap:{ padding: '16px 8px' },
  gaugeNumber:{ fontSize: 36, fontWeight: 700, color: 'var(--ci-text-primary)', lineHeight: 1, marginBottom: 12 },
  gaugeTrack:{ height: 14, background: 'var(--ci-bg-surface-2)', borderRadius: 7, overflow: 'hidden', position: 'relative' },
  gaugeFill: { height: '100%', borderRadius: 7, transition: 'width 0.3s' },
  gaugeMarks:{ display: 'flex', justifyContent: 'space-between', marginTop: 8, fontSize: 10, color: 'var(--ci-text-muted)' },
  gaugeNote: { marginTop: 14, fontSize: 12, color: 'var(--ci-text-secondary)' },
  agingGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 },
  agingCell: { background: 'var(--ci-bg-surface-2)', borderRadius: 8, padding: 16, textAlign: 'center' },
  agingLabel:{ fontSize: 11, color: 'var(--ci-text-muted)', marginBottom: 8 },
  agingValue:{ fontSize: 24, fontWeight: 600 },
};

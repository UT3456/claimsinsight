import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Alert, Skeleton, Popconfirm, Tooltip, Button, App as AntApp } from 'antd';
import { RefreshCw, ShieldAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import {
  PageHeader, KpiCard, DataCard, Badge, DarkButton, EmptyState, CHART,
} from '../../../components/ui';
import { useAuth } from '../../../hooks/useAuth';

import { riskScoresApi, riskIndicatorsApi } from '../../../api/fraudRiskApi';
import { investigationsApi }                from '../../../api/investigationsApi';
import type { RiskScore }                   from '../../../api/fraudRiskApi';
import { settledArray }                     from '../../../utils/settled';

/**
 * FraudDashboard — first-screen view for ROLE_FRAUD_ANALYST.
 *
 * Per SRS Module 4.4 (fraud & risk): the analyst's job is triaging the
 * highest-risk claims and escalating mill-scheme suspects. This page
 * leads with the risk score histogram (portfolio fraud exposure at a
 * glance) and the high-risk queue with an Escalate SIU button on every
 * row — the SRS specifically calls out that without that button the
 * panel is "read-only and useless".
 *
 * KPIs:    High-risk count (≥75) · New flags today · Active indicators · Open investigations
 * Panels:  Risk score distribution histogram (0-25 / 26-50 / 51-75 / 76-100)
 *          High-risk fraud signals queue with Escalate SIU action
 *          Indicator type breakdown (HighCost / UnusualTiming / Pattern)
 */

interface State {
  loading: boolean;
  error: string | null;
  highRiskCount: number;
  newFlagsToday: number;
  activeIndicators: number;
  openInvestigations: number;
  histogram: { bucket: string; count: number; tone: 'good' | 'ok' | 'warn' | 'danger' }[];
  topRisks: RiskScore[];
  indicatorBreakdown: { type: string; count: number }[];
}

type Action = { type: 'START' } | { type: 'OK'; payload: Omit<State, 'loading' | 'error'> } | { type: 'ERR'; payload: string };

const initial: State = {
  loading: false, error: null,
  highRiskCount: 0, newFlagsToday: 0, activeIndicators: 0, openInvestigations: 0,
  histogram: [], topRisks: [], indicatorBreakdown: [],
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START': return { ...s, loading: true, error: null };
    case 'OK':    return { ...s, loading: false, ...a.payload };
    case 'ERR':   return { ...s, loading: false, error: a.payload };
    default:      return s;
  }
}

function isToday(iso: string): boolean {
  const d = new Date(iso);
  const t = new Date();
  return d.getFullYear() === t.getFullYear() && d.getMonth() === t.getMonth() && d.getDate() === t.getDate();
}

export default function FraudDashboard() {
  const { username } = useAuth();
  const navigate = useNavigate();
  const { message } = AntApp.useApp();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const [scoresR, indR, invNewR, invReviewR] = await Promise.allSettled([
        riskScoresApi.getAll(),
        riskIndicatorsApi.getAll(),
        investigationsApi.getAll('NEW'),
        investigationsApi.getAll('UNDER_REVIEW'),
      ]);
      const scores      = settledArray<RiskScore>(scoresR);
      const indicators  = settledArray<{ indicatorType: string; triggeredDate: string }>(indR);
      const invNew      = settledArray<unknown>(invNewR);
      const invReview   = settledArray<unknown>(invReviewR);

      // ── Histogram (portfolio fraud exposure at a glance) ───────────────
      const buckets: { bucket: string; count: number; tone: 'good' | 'ok' | 'warn' | 'danger' }[] = [
        { bucket: '0–25',   count: 0, tone: 'good'   },
        { bucket: '26–50',  count: 0, tone: 'ok'     },
        { bucket: '51–75',  count: 0, tone: 'warn'   },
        { bucket: '76–100', count: 0, tone: 'danger' },
      ];
      for (const s of scores) {
        const v = s.scoreValue;
        const i = v <= 25 ? 0 : v <= 50 ? 1 : v <= 75 ? 2 : 3;
        buckets[i].count++;
      }

      // ── Indicator type breakdown ───────────────────────────────────────
      const tally = new Map<string, number>();
      for (const i of indicators) tally.set(i.indicatorType, (tally.get(i.indicatorType) ?? 0) + 1);
      const indicatorBreakdown = Array.from(tally.entries())
        .map(([type, count]) => ({ type, count }))
        .sort((a, b) => b.count - a.count);

      // ── Top risks (≥75, descending) — these get Escalate buttons ───────
      const topRisks = [...scores]
        .filter(s => s.scoreValue >= 75)
        .sort((a, b) => b.scoreValue - a.scoreValue)
        .slice(0, 6);

      dispatch({
        type: 'OK',
        payload: {
          highRiskCount:      scores.filter(s => s.scoreValue >= 75).length,
          newFlagsToday:      indicators.filter(i => isToday(i.triggeredDate)).length,
          activeIndicators:   indicators.length,
          openInvestigations: invNew.length + invReview.length,
          histogram: buckets,
          topRisks,
          indicatorBreakdown,
        },
      });
    } catch (err) {
      dispatch({ type: 'ERR', payload: (err as { userMessage?: string }).userMessage ?? 'Failed to load dashboard.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const firstName = useMemo(() => (username ?? 'Analyst').split(/[\s.]/)[0], [username]);

  const handleEscalate = async (rec: RiskScore) => {
    try {
      const inv = await investigationsApi.open({
        claimId: rec.claimId,
        riskScoreId: rec.scoreId,
        notes: `Auto-escalation from fraud dashboard, risk score ${Math.round(rec.scoreValue)}.`,
      });
      message.success(`Escalated to SIU — investigation #${inv.investigationId} opened`);
      load();
    } catch (err) {
      const ax = err as { response?: { status?: number }; userMessage?: string };
      if (ax?.response?.status === 409) {
        message.info(ax.userMessage ?? 'Already under investigation.');
      } else {
        message.error(ax.userMessage ?? 'Failed to escalate.');
      }
    }
  };

  return (
    <div>
      <PageHeader
        title="Fraud & risk"
        subtitle={`${firstName} · portfolio risk exposure & SIU queue`}
        actions={<DarkButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>Refresh</DarkButton>}
      />

      {state.error && (
        <Alert type="warning" showIcon closable message={state.error}
          style={{ marginBottom: 16, borderRadius: 8 }} />
      )}

      {/* KPIs */}
      {state.loading ? (
        <div style={styles.kpiGrid}>
          {[0,1,2,3].map(i => <div key={i} style={styles.skel}><Skeleton active paragraph={{ rows: 2 }} title={false} /></div>)}
        </div>
      ) : (
        <div style={styles.kpiGrid}>
          <KpiCard label="High risk (≥75)" value={state.highRiskCount.toLocaleString()}
            tone={state.highRiskCount > 10 ? 'warning' : 'default'}
            delta={state.highRiskCount > 0 ? 'Needs review' : 'Clear'}
            deltaTone={state.highRiskCount > 0 ? 'down' : 'up'} />
          <KpiCard label="New flags today" value={state.newFlagsToday.toLocaleString()} />
          <KpiCard label="Active indicators" value={state.activeIndicators.toLocaleString()} />
          <KpiCard label="Open investigations" value={state.openInvestigations.toLocaleString()}
            delta="NEW + UNDER_REVIEW" deltaDirection="flat" />
        </div>
      )}

      {/* Risk score histogram + indicator breakdown */}
      <div style={styles.twoCol}>
        <DataCard title="Risk score distribution" subtitle="Portfolio fraud exposure">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.histogram.every(b => b.count === 0) ? (
            <EmptyState title="No risk scores" description="Scores appear once claims are evaluated." tone="positive" />
          ) : (
            <div style={styles.histo}>
              {state.histogram.map(b => {
                const max = Math.max(...state.histogram.map(x => x.count), 1);
                const h = (b.count / max) * 100;
                const color = b.tone === 'good' ? CHART.teal
                            : b.tone === 'ok'   ? CHART.blue
                            : b.tone === 'warn' ? CHART.amber : CHART.red;
                return (
                  <div key={b.bucket} style={styles.histoCol}>
                    <div style={styles.histoCount}>{b.count}</div>
                    <div style={styles.histoBarTrack}>
                      <div style={{ ...styles.histoBarFill, height: `${h}%`, background: color }} />
                    </div>
                    <div style={styles.histoLabel}>{b.bucket}</div>
                  </div>
                );
              })}
            </div>
          )}
        </DataCard>

        <DataCard title="Indicator types" subtitle="What's firing">
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 3 }} title={false} />
          ) : state.indicatorBreakdown.length === 0 ? (
            <EmptyState title="No indicators" description="Rule-based flags will appear here when triggered." tone="positive" />
          ) : (
            state.indicatorBreakdown.map((b, i) => {
              const max = Math.max(...state.indicatorBreakdown.map(x => x.count), 1);
              const pct = (b.count / max) * 100;
              const color = i === 0 ? CHART.red : i === 1 ? CHART.amber : '#AFA9EC';
              return (
                <div key={b.type} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span>{b.type}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>{b.count}</span>
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

      {/* Top fraud signals — with Escalate SIU button on every row */}
      <div style={{ marginTop: 12 }}>
        <DataCard
          title="High-risk queue"
          subtitle="Top scoring claims · click Escalate to open an SIU investigation"
          headerRight={
            <a onClick={() => navigate('/fraud-risk')} style={styles.link}>View all →</a>
          }
        >
          {state.loading ? (
            <Skeleton active paragraph={{ rows: 4 }} title={false} />
          ) : state.topRisks.length === 0 ? (
            <EmptyState title="Portfolio clean" description="No claims scored ≥ 75." tone="positive" />
          ) : (
            <div style={styles.queue}>
              {state.topRisks.map(r => {
                const score = Math.round(r.scoreValue);
                const isCritical = score >= 90;
                const tone: 'red' | 'amber' = isCritical ? 'red' : 'amber';
                const chipBg = isCritical ? CHART.red : CHART.amber;
                return (
                  <div key={r.scoreId} style={styles.queueRow}>
                    <div style={{ ...styles.scoreChip, background: chipBg }}>{score}</div>
                    <div style={{ flex: 1 }}>
                      <a onClick={() => navigate(`/claims/${r.claimId}`)} style={styles.claimLink}>
                        {r.claimId}
                      </a>
                      <div style={styles.queueMeta}>
                        <Badge tone={tone}>{isCritical ? 'CRITICAL' : 'HIGH'}</Badge>
                        <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>
                          flagged {new Date(r.computedDate).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                    <Popconfirm
                      title={`Escalate ${r.claimId} to SIU?`}
                      description="Opens an investigation and notifies the fraud manager."
                      okText="Escalate"
                      okButtonProps={{ danger: true }}
                      onConfirm={() => handleEscalate(r)}
                    >
                      <Tooltip title="Escalate to SIU">
                        <Button type="primary" danger size="small"
                          icon={<ShieldAlert size={12} strokeWidth={1.8} />}>
                          Escalate
                        </Button>
                      </Tooltip>
                    </Popconfirm>
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
  kpiGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 12 },
  skel:    { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 10, padding: '16px 20px', minHeight: 88 },
  twoCol:  { display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 12 },
  barRow:  { marginBottom: 10 },
  barHead: { display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 4 },
  barTrack:{ height: 8, background: 'var(--ci-bg-surface-2)', borderRadius: 4, overflow: 'hidden' },
  barFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
  histo:    { display: 'flex', alignItems: 'flex-end', gap: 12, height: 180, padding: '8px 0' },
  histoCol: { flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 },
  histoCount:{ fontSize: 12, fontWeight: 600, color: 'var(--ci-text-primary)' },
  histoBarTrack: { width: '100%', height: 130, background: 'var(--ci-bg-surface-2)', borderRadius: 4, display: 'flex', alignItems: 'flex-end', overflow: 'hidden' },
  histoBarFill:  { width: '100%', borderRadius: 4, transition: 'height 0.3s' },
  histoLabel:{ fontSize: 10, color: 'var(--ci-text-muted)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' },
  queue:    { display: 'flex', flexDirection: 'column', gap: 8 },
  queueRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '10px 12px', background: 'var(--ci-bg-surface-2)', borderRadius: 8 },
  queueMeta:{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 2 },
  scoreChip:{ width: 40, height: 32, borderRadius: 6, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600 },
  claimLink:{ fontSize: 13, fontWeight: 500, color: 'var(--ci-primary)', cursor: 'pointer' },
  link:     { fontSize: 11, color: 'var(--ci-primary)', cursor: 'pointer', textDecoration: 'none' },
};

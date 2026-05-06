import { useEffect, useMemo, useReducer, useCallback } from 'react';
import {
  Table, Alert, Tooltip, Popconfirm, Button, Dropdown,
  App as AntApp,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { useNavigate } from 'react-router-dom';
import { Trash2, ShieldAlert } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, GhostButton, EmptyState, CHART,
} from '../../components/ui';
import {
  riskScoresApi, riskIndicatorsApi,
  type RiskScore, type RiskIndicator,
  INDICATOR_TYPES,
} from '../../api/fraudRiskApi';
import { investigationsApi } from '../../api/investigationsApi';

/**
 * FraudRiskPage — reference screen #04 "Fraud & risk".
 *
 * Layout: PageHeader · 3 KPI cards (red/amber/default) · two-col
 * (Risk score histogram + Indicator breakdown) · High-risk claim queue table.
 *
 * Add Score / Add Indicator modals are preserved (admin add actions live in
 * a dropdown next to the queue title rather than the page header — keeps
 * the header focused on filters per the reference).
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

type FilterScore = 'ALL' | 'GTE_70' | 'GTE_85' | 'CRITICAL';
type FilterIndicator = 'ALL' | typeof INDICATOR_TYPES[number];
type FilterPeriod = 'D7' | 'D30' | 'D90';

interface State {
  scores: RiskScore[];
  indicators: RiskIndicator[];
  loadingScores: boolean;
  loadingIndicators: boolean;
  error: string | null;
  filterScore: FilterScore;
  filterIndicator: FilterIndicator;
  filterPeriod: FilterPeriod;
}

type Action =
  | { type: 'SCORES_START' }
  | { type: 'SCORES_OK'; payload: RiskScore[] }
  | { type: 'INDICATORS_START' }
  | { type: 'INDICATORS_OK'; payload: RiskIndicator[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'DELETE_SCORE'; payload: number }
  | { type: 'DELETE_INDICATOR'; payload: number }
  | { type: 'SET_FS'; payload: FilterScore }
  | { type: 'SET_FI'; payload: FilterIndicator }
  | { type: 'SET_FP'; payload: FilterPeriod };

const initial: State = {
  scores: [], indicators: [],
  loadingScores: false, loadingIndicators: false,
  error: null,
  filterScore: 'GTE_70', filterIndicator: 'ALL', filterPeriod: 'D7',
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'SCORES_START':       return { ...s, loadingScores: true, error: null };
    case 'SCORES_OK':          return { ...s, loadingScores: false, scores: a.payload };
    case 'INDICATORS_START':   return { ...s, loadingIndicators: true, error: null };
    case 'INDICATORS_OK':      return { ...s, loadingIndicators: false, indicators: a.payload };
    case 'ERROR':              return { ...s, loadingScores: false, loadingIndicators: false, error: a.payload };
    case 'DELETE_SCORE':       return { ...s, scores: s.scores.filter(x => x.scoreId !== a.payload) };
    case 'DELETE_INDICATOR':   return { ...s, indicators: s.indicators.filter(x => x.indicatorId !== a.payload) };
    case 'SET_FS':             return { ...s, filterScore: a.payload };
    case 'SET_FI':             return { ...s, filterIndicator: a.payload };
    case 'SET_FP':             return { ...s, filterPeriod: a.payload };
    default:                   return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function relativeFlag(iso: string): string {
  const t = new Date(iso).getTime();
  if (isNaN(t)) return '—';
  const diffMin = Math.max(0, Math.floor((Date.now() - t) / 60000));
  if (diffMin < 60)         return `${diffMin}m ago`;
  if (diffMin < 60 * 24)    return `${Math.floor(diffMin / 60)}h ago`;
  return `${Math.floor(diffMin / (60 * 24))}d ago`;
}

const SCORE_FILTER_LABEL: Record<FilterScore, string> = {
  ALL:      'All scores',
  GTE_70:   'Risk ≥ 70',
  GTE_85:   'Risk ≥ 85',
  CRITICAL: 'Critical (≥90)',
};

const PERIOD_LABEL: Record<FilterPeriod, string> = {
  D7:  'Last 7 days',
  D30: 'Last 30 days',
  D90: 'Last 90 days',
};

const PERIOD_DAYS: Record<FilterPeriod, number> = { D7: 7, D30: 30, D90: 90 };

// ── Component ────────────────────────────────────────────────────────────────

export default function FraudRiskPage() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, initial);
  const { message } = AntApp.useApp();

  const loadScores = useCallback(async () => {
    dispatch({ type: 'SCORES_START' });
    try { dispatch({ type: 'SCORES_OK', payload: await riskScoresApi.getAll() }); }
    catch { dispatch({ type: 'ERROR', payload: 'Failed to load risk scores.' }); }
  }, []);

  const loadIndicators = useCallback(async () => {
    dispatch({ type: 'INDICATORS_START' });
    try { dispatch({ type: 'INDICATORS_OK', payload: await riskIndicatorsApi.getAll() }); }
    catch { dispatch({ type: 'ERROR', payload: 'Failed to load risk indicators.' }); }
  }, []);

  useEffect(() => { loadScores(); loadIndicators(); }, [loadScores, loadIndicators]);

  // ── Escalate SIU ──────────────────────────────────────────────────────────
  // Opens a fraud investigation on the selected claim. Backend persists the
  // Investigation row + dispatches a notification to FRAUD + ADMIN bells.
  // Conflict (409) is shown as a non-fatal info — the claim already has an
  // open investigation so no duplicate is needed.
  const handleEscalate = async (rec: RiskScore) => {
    try {
      const inv = await investigationsApi.open({
        claimId:     rec.claimId,
        riskScoreId: rec.scoreId,
        notes:       `Auto-escalation from risk score ${Math.round(rec.scoreValue)}.`,
      });
      message.success(`Escalated to SIU — investigation #${inv.investigationId} opened`);
    } catch (err) {
      const ax = err as { response?: { status?: number }, userMessage?: string };
      if (ax?.response?.status === 409) {
        message.info(ax.userMessage ?? 'This claim is already under investigation.');
      } else {
        message.error(ax.userMessage ?? 'Failed to escalate to SIU.');
      }
    }
  };

  // ── Derived ───────────────────────────────────────────────────────────────

  const periodCutoff = Date.now() - PERIOD_DAYS[state.filterPeriod] * 86_400_000;

  const indicatorsInPeriod = useMemo(
    () => state.indicators.filter(i => new Date(i.triggeredDate).getTime() >= periodCutoff),
    [state.indicators, periodCutoff],
  );

  const highRiskScores = useMemo(() => state.scores.filter(s => s.scoreValue >= 75), [state.scores]);
  const newFlagsCount  = indicatorsInPeriod.length;

  // Indicator-type breakdown — bars colored red/amber/amber/light by rank.
  const indicatorBreakdown = useMemo(() => {
    const tally = new Map<string, number>();
    for (const i of indicatorsInPeriod) tally.set(i.indicatorType, (tally.get(i.indicatorType) ?? 0) + 1);
    const sorted = Array.from(tally.entries())
      .map(([type, count]) => ({ type, count }))
      .sort((a, b) => b.count - a.count);
    const palette = [CHART.red, CHART.amber, CHART.amber, '#FAC775'];
    return sorted.map((row, idx) => ({ ...row, color: palette[idx] ?? '#FAC775' }));
  }, [indicatorsInPeriod]);

  // High-risk queue — top scores filtered by score threshold + indicator type
  const filteredQueue = useMemo(() => {
    const min = state.filterScore === 'CRITICAL' ? 90
              : state.filterScore === 'GTE_85'   ? 85
              : state.filterScore === 'GTE_70'   ? 70
              : 0;
    return state.scores
      .filter(s => s.scoreValue >= min)
      .filter(s => {
        if (state.filterIndicator === 'ALL') return true;
        return state.indicators.some(i => i.claimId === s.claimId && i.indicatorType === state.filterIndicator);
      })
      .sort((a, b) => b.scoreValue - a.scoreValue);
  }, [state.scores, state.indicators, state.filterScore, state.filterIndicator]);

  // Indicator string per claim — joined types for display
  const indicatorByClaim = useMemo(() => {
    const map = new Map<string, string[]>();
    for (const i of state.indicators) {
      const arr = map.get(i.claimId) ?? [];
      arr.push(i.indicatorType);
      map.set(i.claimId, arr);
    }
    return map;
  }, [state.indicators]);

  // ── Filter chip menus ────────────────────────────────────────────────────

  const scoreMenu: MenuProps = {
    items: (['GTE_70', 'GTE_85', 'CRITICAL', 'ALL'] as FilterScore[]).map(k => ({
      key: k, label: SCORE_FILTER_LABEL[k],
    })),
    selectable: true,
    selectedKeys: [state.filterScore],
    onClick: ({ key }) => dispatch({ type: 'SET_FS', payload: key as FilterScore }),
  };
  const indicatorMenu: MenuProps = {
    items: (['ALL', ...INDICATOR_TYPES] as FilterIndicator[]).map(k => ({
      key: k, label: k === 'ALL' ? 'All indicators' : k,
    })),
    selectable: true,
    selectedKeys: [state.filterIndicator],
    onClick: ({ key }) => dispatch({ type: 'SET_FI', payload: key as FilterIndicator }),
  };
  const periodMenu: MenuProps = {
    items: (['D7', 'D30', 'D90'] as FilterPeriod[]).map(k => ({
      key: k, label: PERIOD_LABEL[k],
    })),
    selectable: true,
    selectedKeys: [state.filterPeriod],
    onClick: ({ key }) => dispatch({ type: 'SET_FP', payload: key as FilterPeriod }),
  };

  // ── Table columns ────────────────────────────────────────────────────────

  const queueColumns: ColumnsType<RiskScore> = [
    {
      title: 'Claim', dataIndex: 'claimId', key: 'claimId', width: '20%',
      render: (v: string) => (
        <a onClick={() => navigate(`/claims/${v}`)} style={{ fontWeight: 500, color: 'var(--ci-text-primary)' }}>
          {v}
        </a>
      ),
    },
    {
      title: 'Indicator', key: 'indicator', width: '30%',
      render: (_, rec) => {
        const types = indicatorByClaim.get(rec.claimId);
        if (!types || types.length === 0) {
          return <span style={{ color: 'var(--ci-text-muted)' }}>—</span>;
        }
        return <span style={{ color: 'var(--ci-text-secondary)' }}>{types.join(' · ')}</span>;
      },
    },
    {
      title: 'Score', dataIndex: 'scoreValue', key: 'scoreValue', width: '12%', align: 'center',
      sorter: (a, b) => a.scoreValue - b.scoreValue,
      render: (v: number) => {
        const n = Math.round(v);
        const tone = n >= 90 ? 'red' : n >= 75 ? 'amber' : 'blue';
        return <Badge tone={tone}>{n}</Badge>;
      },
    },
    {
      title: 'Flagged', dataIndex: 'computedDate', key: 'flagged', width: '15%',
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{relativeFlag(v)}</span>,
    },
    {
      title: 'Assigned', key: 'assigned', width: '13%',
      render: () => <span style={{ color: 'var(--ci-text-muted)' }}>—</span>,
    },
    {
      title: '', key: 'action', width: '18%', align: 'right',
      render: (_, rec) => (
        <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
          <Popconfirm
            title={`Escalate claim ${rec.claimId} to SIU?`}
            description="Opens an investigation and notifies the SIU/Fraud manager."
            okText="Escalate"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleEscalate(rec)}
          >
            <Tooltip title="Escalate to SIU">
              <Button
                type="primary" danger size="small"
                icon={<ShieldAlert size={12} strokeWidth={1.8} />}
              >
                Escalate
              </Button>
            </Tooltip>
          </Popconfirm>
          <Popconfirm
            title="Delete this risk score?"
            onConfirm={async () => {
              await riskScoresApi.delete(rec.scoreId);
              dispatch({ type: 'DELETE_SCORE', payload: rec.scoreId });
            }}
          >
            <Tooltip title="Delete">
              <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
            </Tooltip>
          </Popconfirm>
        </div>
      ),
    },
  ];

  // ── Render ───────────────────────────────────────────────────────────────

  return (
    <div>
      <PageHeader
        title="Fraud & risk"
        subtitle={`${highRiskScores.length} active high-risk claims · ${newFlagsCount} new flag${newFlagsCount === 1 ? '' : 's'} (${PERIOD_LABEL[state.filterPeriod].toLowerCase()})`}
        actions={
          <>
            <Dropdown menu={scoreMenu} trigger={['click']}>
              <Chip dropdown active={state.filterScore !== 'ALL'}>
                {SCORE_FILTER_LABEL[state.filterScore]}
              </Chip>
            </Dropdown>
            <Dropdown menu={indicatorMenu} trigger={['click']}>
              <Chip dropdown active={state.filterIndicator !== 'ALL'}>
                {state.filterIndicator === 'ALL' ? 'Indicator type' : state.filterIndicator}
              </Chip>
            </Dropdown>
            <Dropdown menu={periodMenu} trigger={['click']}>
              <Chip dropdown>
                {PERIOD_LABEL[state.filterPeriod]}
              </Chip>
            </Dropdown>
          </>
        }
      />

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      {/* KPI row — 3 cards (red, amber, default) */}
      <div style={styles.kpiGrid3}>
        <KpiCard
          tone="danger"
          label="High-risk claims"
          value={state.loadingScores ? '—' : highRiskScores.length.toLocaleString()}
          delta={newFlagsCount > 0 ? `${newFlagsCount} in ${PERIOD_DAYS[state.filterPeriod]}d` : undefined}
          deltaDirection="up"
          deltaTone="down"
        />
        <KpiCard
          tone="warning"
          label={`New flags (${PERIOD_DAYS[state.filterPeriod]}d)`}
          value={state.loadingIndicators ? '—' : newFlagsCount.toLocaleString()}
          delta={state.indicators.length > 0 ? `${Math.round((newFlagsCount / state.indicators.length) * 100)}% of total` : undefined}
          deltaDirection="up"
          deltaTone="down"
        />
        <KpiCard
          label="Avg risk score"
          value={state.loadingScores
            ? '—'
            : state.scores.length === 0
              ? '—'
              : (state.scores.reduce((a, s) => a + s.scoreValue, 0) / state.scores.length).toFixed(1)}
          delta={state.scores.length > 0 ? `across ${state.scores.length} claims` : undefined}
          deltaDirection="up"
          deltaTone="up"
        />
      </div>

      {/* Indicator breakdown */}
      <div style={{ marginBottom: 12 }}>
        <DataCard title="Indicator breakdown" subtitle={`Triggered ${PERIOD_LABEL[state.filterPeriod].toLowerCase()}`}>
          {state.loadingIndicators ? (
            <div style={styles.placeholder}>Loading…</div>
          ) : indicatorBreakdown.length === 0 ? (
            <EmptyState
              title="No indicators triggered"
              description="No fraud signals fired in this period."
              tone="positive"
            />
          ) : (
            indicatorBreakdown.map((row) => {
              const max = Math.max(...indicatorBreakdown.map(x => x.count), 1);
              const pct = (row.count / max) * 100;
              return (
                <div key={row.type} style={styles.barRow}>
                  <div style={styles.barHead}>
                    <span>{row.type}</span>
                    <span style={{ color: 'var(--ci-text-muted)' }}>{row.count}</span>
                  </div>
                  <div style={styles.barTrack}>
                    <div style={{ ...styles.barFill, width: `${pct}%`, background: row.color }} />
                  </div>
                </div>
              );
            })
          )}
        </DataCard>
      </div>

      {/* High-risk claim queue */}
      <DataCard
        title="High-risk claim queue"
        subtitle={`${filteredQueue.length} matching claim${filteredQueue.length === 1 ? '' : 's'}`}
        headerRight={
          <GhostButton onClick={() => { loadScores(); loadIndicators(); }}>Refresh</GhostButton>
        }
        padding={0}
      >
        {filteredQueue.length === 0 && !state.loadingScores ? (
          <EmptyState
            title="No claims in queue"
            description="Try lowering the risk threshold or expanding the indicator filter."
            tone="positive"
            actions={
              state.filterScore !== 'ALL' || state.filterIndicator !== 'ALL'
                ? <GhostButton onClick={() => { dispatch({ type: 'SET_FS', payload: 'ALL' }); dispatch({ type: 'SET_FI', payload: 'ALL' }); }}>Reset filters</GhostButton>
                : undefined
            }
          />
        ) : (
          <Table
            rowKey="scoreId"
            columns={queueColumns}
            dataSource={filteredQueue}
            loading={state.loadingScores}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
          />
        )}
      </DataCard>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiGrid3: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)',
    gap: 12, marginBottom: 12,
  },
  placeholder: {
    height: 160, display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: 'var(--ci-text-muted)', fontSize: 11,
  },
  barRow: { marginBottom: 10 },
  barHead: {
    display: 'flex', justifyContent: 'space-between',
    fontSize: 11, marginBottom: 4,
  },
  barTrack: {
    height: 8, background: 'var(--ci-bg-surface-2)',
    borderRadius: 4, overflow: 'hidden',
  },
  barFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
};

import { useEffect, useMemo, useReducer, useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Alert, Popconfirm, Tooltip, Button,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefreshCw, Trash2 } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, StatusDot, GhostButton, EmptyState,
} from '../../components/ui';
import type { BadgeTone, StatusTone } from '../../components/ui';
import {
  denialPatternsApi, leakageFlagsApi, LEAKAGE_TYPES,
  type DenialPattern, type LeakageFlag,
} from '../../api/denialLeakageApi';

/**
 * DenialLeakagePage — reference screen #05.
 * Layout: PageHeader · 3 KpiCards · two-col (top denial codes bar · leakage by type bar)
 *         · DataCard with tab switch between Patterns / Flags tables.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  patterns: DenialPattern[];
  flags: LeakageFlag[];
  loadingPatterns: boolean;
  loadingFlags: boolean;
  error: string | null;
  tab: 'patterns' | 'flags';
  typeFilter: 'ALL' | typeof LEAKAGE_TYPES[number];
}

type Action =
  | { type: 'PATTERNS_START' }
  | { type: 'PATTERNS_SUCCESS'; payload: DenialPattern[] }
  | { type: 'FLAGS_START' }
  | { type: 'FLAGS_SUCCESS'; payload: LeakageFlag[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'DELETE_PATTERN'; payload: number }
  | { type: 'DELETE_FLAG'; payload: number }
  | { type: 'SET_TAB'; payload: State['tab'] }
  | { type: 'SET_TYPE_FILTER'; payload: State['typeFilter'] };

const initial: State = {
  patterns: [], flags: [],
  loadingPatterns: false, loadingFlags: false,
  error: null,
  tab: 'patterns', typeFilter: 'ALL',
};

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'PATTERNS_START':      return { ...s, loadingPatterns: true, error: null };
    case 'PATTERNS_SUCCESS':    return { ...s, loadingPatterns: false, patterns: a.payload };
    case 'FLAGS_START':         return { ...s, loadingFlags: true, error: null };
    case 'FLAGS_SUCCESS':       return { ...s, loadingFlags: false, flags: a.payload };
    case 'FETCH_ERROR':         return { ...s, loadingPatterns: false, loadingFlags: false, error: a.payload };
    case 'DELETE_PATTERN':      return { ...s, patterns: s.patterns.filter(p => p.patternId !== a.payload) };
    case 'DELETE_FLAG':         return { ...s, flags: s.flags.filter(f => f.leakageId !== a.payload) };
    case 'SET_TAB':             return { ...s, tab: a.payload };
    case 'SET_TYPE_FILTER':     return { ...s, typeFilter: a.payload };
    default:                    return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const LEAKAGE_META: Record<string, { tone: BadgeTone; status: StatusTone }> = {
  Overpayment: { tone: 'red',   status: 'danger'  },
  Delay:       { tone: 'amber', status: 'warning' },
  Error:       { tone: 'purple', status: 'info'   },
};

const fmtUSD = (n: number) =>
  `$${n.toLocaleString('en-US', { maximumFractionDigits: 0 })}`;

// ── Component ────────────────────────────────────────────────────────────────

export default function DenialLeakagePage() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, initial);
  const [selectedCode, setSelectedCode] = useState<string | null>(null);

  const loadPatterns = useCallback(async () => {
    dispatch({ type: 'PATTERNS_START' });
    try {
      dispatch({ type: 'PATTERNS_SUCCESS', payload: await denialPatternsApi.getAll() });
    } catch { dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load denial patterns.' }); }
  }, []);

  const loadFlags = useCallback(async () => {
    dispatch({ type: 'FLAGS_START' });
    try {
      dispatch({ type: 'FLAGS_SUCCESS', payload: await leakageFlagsApi.getAll() });
    } catch { dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load leakage flags.' }); }
  }, []);

  useEffect(() => { loadPatterns(); loadFlags(); }, [loadPatterns, loadFlags]);

  // ── Derived data ─────────────────────────────────────────────────────────
  const totalLeakage = state.flags.reduce((s, f) => s + (f.estimatedLoss ?? 0), 0);
  const avgLoss = state.flags.length ? totalLeakage / state.flags.length : 0;

  const filteredFlags = useMemo(() => (
    state.typeFilter === 'ALL' ? state.flags : state.flags.filter(f => f.leakageType === state.typeFilter)
  ), [state.flags, state.typeFilter]);

  const filteredPatterns = useMemo(() => (
    selectedCode ? state.patterns.filter(p => p.denialCode === selectedCode) : state.patterns
  ), [state.patterns, selectedCode]);

  // ── Columns ──────────────────────────────────────────────────────────────
  const patternColumns: ColumnsType<DenialPattern> = [
    {
      title: 'ID', dataIndex: 'patternId', key: 'patternId', width: 70,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim', dataIndex: 'claimId', key: 'claimId', width: 180,
      render: v => (
        <a
          onClick={() => navigate(`/claims/${v}`)}
          style={{ color: 'var(--ci-primary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 11 }}
        >{v}</a>
      ),
    },
    {
      title: 'Code', dataIndex: 'denialCode', key: 'denialCode', width: 90,
      render: v => <Badge tone="red">{v}</Badge>,
    },
    { title: 'Reason', dataIndex: 'reason', key: 'reason', ellipsis: true,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span> },
    { title: 'Date', dataIndex: 'occurrenceDate', key: 'occurrenceDate', width: 110,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span> },
    {
      title: '', key: 'action', width: 40, align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this pattern?" onConfirm={async () => {
          await denialPatternsApi.delete(rec.patternId);
          dispatch({ type: 'DELETE_PATTERN', payload: rec.patternId });
        }}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const flagColumns: ColumnsType<LeakageFlag> = [
    {
      title: 'ID', dataIndex: 'leakageId', key: 'leakageId', width: 70,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim', dataIndex: 'claimId', key: 'claimId', width: 180,
      render: v => (
        <a
          onClick={() => navigate(`/claims/${v}`)}
          style={{ color: 'var(--ci-primary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 11 }}
        >{v}</a>
      ),
    },
    {
      title: 'Type', dataIndex: 'leakageType', key: 'leakageType', width: 140,
      render: (v: string) => {
        const meta = LEAKAGE_META[v] ?? { tone: 'neutral' as BadgeTone, status: 'muted' as StatusTone };
        return <StatusDot tone={meta.status}>{v}</StatusDot>;
      },
    },
    {
      title: 'Estimated loss', dataIndex: 'estimatedLoss', key: 'estimatedLoss', width: 150,
      sorter: (a, b) => a.estimatedLoss - b.estimatedLoss,
      render: v => <span style={{ fontWeight: 500, color: 'var(--ci-danger-text, #B22C2B)' }}>{fmtUSD(Number(v))}</span>,
    },
    { title: 'Identified', dataIndex: 'identifiedDate', key: 'identifiedDate', width: 120,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span> },
    {
      title: '', key: 'action', width: 40, align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this flag?" onConfirm={async () => {
          await leakageFlagsApi.delete(rec.leakageId);
          dispatch({ type: 'DELETE_FLAG', payload: rec.leakageId });
        }}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const typeCycle: State['typeFilter'][] = ['ALL', 'Overpayment', 'Delay', 'Error'];

  return (
    <div>
      <PageHeader
        title="Denial & leakage"
        subtitle={`${state.patterns.length} pattern${state.patterns.length === 1 ? '' : 's'} · ${fmtUSD(totalLeakage)} estimated loss`}
        actions={
          <>
            <Chip
              dropdown
              active={state.typeFilter !== 'ALL'}
              onClick={() => {
                const i = typeCycle.indexOf(state.typeFilter);
                dispatch({ type: 'SET_TYPE_FILTER', payload: typeCycle[(i + 1) % typeCycle.length] });
              }}
            >
              {state.typeFilter === 'ALL' ? 'All types' : state.typeFilter}
            </Chip>
            <GhostButton onClick={() => { loadPatterns(); loadFlags(); }} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard label="Denial patterns" value={state.patterns.length.toLocaleString()} delta="Active tracking" deltaDirection="flat" />
        <KpiCard label="Leakage flags"   value={state.flags.length.toLocaleString()}     delta={state.flags.length > 0 ? 'Under review' : 'None open'} deltaDirection="flat" tone={state.flags.length > 0 ? 'warning' : 'default'} />
        <KpiCard label="Total leakage"   value={fmtUSD(totalLeakage)}                    delta={`Avg ${fmtUSD(avgLoss)} per flag`} deltaDirection="flat" tone={totalLeakage > 0 ? 'danger' : 'default'} />
      </div>

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      {/* Table switcher */}
      <div style={styles.tabRow}>
        <button
          onClick={() => dispatch({ type: 'SET_TAB', payload: 'patterns' })}
          style={{ ...styles.tab, ...(state.tab === 'patterns' ? styles.tabActive : {}) }}
        >
          Denial patterns <span style={styles.tabCount}>{state.patterns.length}</span>
        </button>
        <button
          onClick={() => dispatch({ type: 'SET_TAB', payload: 'flags' })}
          style={{ ...styles.tab, ...(state.tab === 'flags' ? styles.tabActive : {}) }}
        >
          Leakage flags <span style={styles.tabCount}>{state.flags.length}</span>
        </button>
      </div>

      <DataCard padding={0}>
        {state.tab === 'patterns' ? (
          !state.loadingPatterns && filteredPatterns.length === 0 ? (
            <EmptyState
              title={selectedCode ? `No patterns for ${selectedCode}` : 'No denial patterns'}
              description={selectedCode ? 'Try clearing the chart filter.' : 'Patterns will appear as denials are recorded.'}
              actions={selectedCode ? <GhostButton onClick={() => setSelectedCode(null)}>Clear filter</GhostButton> : undefined}
            />
          ) : (
            <Table
              rowKey="patternId"
              columns={patternColumns}
              dataSource={filteredPatterns}
              loading={state.loadingPatterns}
              size="small"
              pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            />
          )
        ) : (
          !state.loadingFlags && filteredFlags.length === 0 ? (
            <EmptyState
              title={state.typeFilter === 'ALL' ? 'No leakage flags' : `No ${state.typeFilter} flags`}
              description={state.typeFilter === 'ALL' ? 'Flags will appear once leakage is identified.' : 'Try a different type filter.'}
              tone={state.typeFilter === 'ALL' ? 'positive' : 'neutral'}
            />
          ) : (
            <Table
              rowKey="leakageId"
              columns={flagColumns}
              dataSource={filteredFlags}
              loading={state.loadingFlags}
              size="small"
              pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            />
          )
        )}
      </DataCard>

    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiRow: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 16,
  },
  tabRow: {
    display: 'flex', gap: 4, marginBottom: 10, borderBottom: '1px solid var(--ci-border)',
  },
  tab: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    background: 'transparent', border: 'none',
    padding: '8px 10px', fontSize: 12, fontWeight: 500,
    color: 'var(--ci-text-muted)', cursor: 'pointer',
    borderBottom: '2px solid transparent', marginBottom: -1,
  },
  tabActive: {
    color: 'var(--ci-text-primary)',
    borderBottomColor: 'var(--ci-primary)',
  },
  tabCount: {
    fontSize: 10, fontWeight: 500,
    background: 'var(--ci-bg-surface-2)',
    color: 'var(--ci-text-muted)',
    padding: '1px 6px', borderRadius: 10,
  },
};

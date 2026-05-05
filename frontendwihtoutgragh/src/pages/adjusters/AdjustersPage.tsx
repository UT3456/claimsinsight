import { useEffect, useMemo, useReducer, useCallback } from 'react';
import {
  Table, Alert, Tooltip, Popconfirm, Button,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefreshCw, Trash2, Trophy, Search } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, StatusDot, TintedAvatar,
  GhostButton, EmptyState, CHART,
} from '../../components/ui';
import type { BadgeTone, StatusTone } from '../../components/ui';
import { adjustersApi, type AdjusterPerformance } from '../../api/adjustersApi';

/**
 * AdjustersPage — reference screen #06.
 * Layout: PageHeader · 4 KpiCards · two-col charts · DataCard table with avatars.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: AdjusterPerformance[];
  loading: boolean;
  error: string | null;
  period: string;
  tierFilter: 'ALL' | 'TOP' | 'MID' | 'LOW';
  search: string;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: AdjusterPerformance[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'DELETE'; payload: number }
  | { type: 'SET_PERIOD'; payload: string }
  | { type: 'SET_TIER'; payload: State['tierFilter'] }
  | { type: 'SET_SEARCH'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'FETCH_START':   return { ...s, loading: true, error: null };
    case 'FETCH_SUCCESS': return { ...s, loading: false, items: a.payload };
    case 'FETCH_ERROR':   return { ...s, loading: false, error: a.payload };
    case 'DELETE':        return { ...s, items: s.items.filter(i => i.perfId !== a.payload) };
    case 'SET_PERIOD':    return { ...s, period: a.payload };
    case 'SET_TIER':      return { ...s, tierFilter: a.payload };
    case 'SET_SEARCH':    return { ...s, search: a.payload };
    default:              return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function tierOf(slaRate: number): 'TOP' | 'MID' | 'LOW' {
  if (slaRate >= 90) return 'TOP';
  if (slaRate >= 75) return 'MID';
  return 'LOW';
}

function flagMeta(flag: string): { tone: BadgeTone; status: StatusTone } {
  if (!flag) return { tone: 'neutral', status: 'muted' };
  const f = flag.toLowerCase();
  if (f.includes('high') || f.includes('good') || f.includes('top'))
    return { tone: 'green', status: 'healthy' };
  if (f.includes('low') || f.includes('poor'))
    return { tone: 'red', status: 'danger' };
  return { tone: 'amber', status: 'warning' };
}

// ── Component ────────────────────────────────────────────────────────────────

export default function AdjustersPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null,
    period: '', tierFilter: 'ALL', search: '',
  });

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await adjustersApi.getAll(state.period || undefined);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load adjuster performance.' });
    }
  }, [state.period]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: number) => {
    await adjustersApi.delete(id);
    dispatch({ type: 'DELETE', payload: id });
  };

  // ── Derived ──────────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    let list = state.items;
    if (state.tierFilter !== 'ALL') {
      list = list.filter(i => tierOf(i.slaComplianceRate ?? 0) === state.tierFilter);
    }
    if (state.search.trim()) {
      const q = state.search.trim().toLowerCase();
      list = list.filter(i => String(i.adjusterId).includes(q) || (i.period ?? '').toLowerCase().includes(q));
    }
    return list;
  }, [state.items, state.tierFilter, state.search]);

  const avgTat = state.items.length
    ? (state.items.reduce((s, i) => s + (i.avgTat ?? 0), 0) / state.items.length)
    : 0;
  const avgSla = state.items.length
    ? (state.items.reduce((s, i) => s + (i.slaComplianceRate ?? 0), 0) / state.items.length)
    : 0;
  const topCount = state.items.filter(i => (i.slaComplianceRate ?? 0) >= 90).length;

  const columns: ColumnsType<AdjusterPerformance> = [
    {
      title: 'Adjuster', dataIndex: 'adjusterId', key: 'adjusterId', width: 180,
      render: (id: number) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <TintedAvatar size={28} name={`A${id}`} colorKey={String(id)} />
          <div>
            <div style={{ fontWeight: 500, fontSize: 12 }}>Adjuster #{id}</div>
            <div style={{ fontSize: 10, color: 'var(--ci-text-muted)' }}>ID {id}</div>
          </div>
        </div>
      ),
    },
    {
      title: 'Period', dataIndex: 'period', key: 'period', width: 100,
      render: v => <Badge tone="purple">{v}</Badge>,
    },
    {
      title: 'Claims', dataIndex: 'claimsHandled', key: 'claimsHandled', width: 90,
      sorter: (a, b) => a.claimsHandled - b.claimsHandled,
      render: v => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Avg TAT', dataIndex: 'avgTat', key: 'avgTat', width: 90,
      sorter: (a, b) => (a.avgTat ?? 0) - (b.avgTat ?? 0),
      render: v => (
        <span style={{ color: (v ?? 0) > 10 ? 'var(--ci-warning-text)' : 'var(--ci-text-secondary)' }}>
          {Number(v ?? 0).toFixed(1)}d
        </span>
      ),
    },
    {
      title: 'SLA compliance', dataIndex: 'slaComplianceRate', key: 'slaComplianceRate', width: 180,
      sorter: (a, b) => (a.slaComplianceRate ?? 0) - (b.slaComplianceRate ?? 0),
      render: (v: number) => {
        const rate = Math.round(v ?? 0);
        const color = rate >= 90 ? CHART.teal : rate >= 75 ? CHART.amber : CHART.red;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={styles.slaBarTrack}>
              <div style={{ ...styles.slaBarFill, width: `${Math.min(rate, 100)}%`, background: color }} />
            </div>
            <span style={{ fontSize: 11, fontWeight: 500, width: 32, textAlign: 'right', color: 'var(--ci-text-secondary)' }}>{rate}%</span>
          </div>
        );
      },
    },
    {
      title: 'Error rate', dataIndex: 'errorRate', key: 'errorRate', width: 90,
      render: v => (
        <span style={{ color: (v ?? 0) > 5 ? 'var(--ci-danger-text, #B22C2B)' : 'var(--ci-text-secondary)' }}>
          {Number(v ?? 0).toFixed(1)}%
        </span>
      ),
    },
    {
      title: 'Performance', dataIndex: 'performanceFlag', key: 'performanceFlag', width: 140,
      render: (v: string) => {
        if (!v) return <span style={{ color: 'var(--ci-text-muted)' }}>—</span>;
        const meta = flagMeta(v);
        return <StatusDot tone={meta.status}>{v}</StatusDot>;
      },
    },
    {
      title: '', key: 'action', width: 40, align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this record?" onConfirm={() => handleDelete(rec.perfId)}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const tierCycle: State['tierFilter'][] = ['ALL', 'TOP', 'MID', 'LOW'];
  const tierLabel: Record<State['tierFilter'], string> = {
    ALL: 'All tiers', TOP: 'Top (≥90%)', MID: 'Mid (75–90%)', LOW: 'Low (<75%)',
  };

  return (
    <div>
      <PageHeader
        title="Adjusters"
        subtitle={`${state.items.length} record${state.items.length === 1 ? '' : 's'} · ${topCount} top performer${topCount === 1 ? '' : 's'}`}
        actions={
          <>
            <Chip
              dropdown
              active={state.tierFilter !== 'ALL'}
              onClick={() => {
                const i = tierCycle.indexOf(state.tierFilter);
                dispatch({ type: 'SET_TIER', payload: tierCycle[(i + 1) % tierCycle.length] });
              }}
            >
              {tierLabel[state.tierFilter]}
            </Chip>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard label="Adjusters tracked" value={state.items.length.toLocaleString()} delta="Active handlers" deltaDirection="flat" />
        <KpiCard label="Avg TAT"          value={`${avgTat.toFixed(1)}d`} delta={avgTat > 10 ? 'Above target' : 'On target'} deltaDirection={avgTat > 10 ? 'up' : 'down'} deltaTone={avgTat > 10 ? 'down' : 'up'} />
        <KpiCard label="Avg SLA"          value={`${avgSla.toFixed(0)}%`} delta={avgSla >= 85 ? 'Healthy' : 'Needs attention'} deltaDirection={avgSla >= 85 ? 'up' : 'down'} deltaTone={avgSla >= 85 ? 'up' : 'down'} />
        <KpiCard label="Top performers"   value={topCount.toLocaleString()} delta="SLA ≥ 90%" deltaDirection="flat" />
      </div>

      {state.error && <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />}

      {/* Filter rail */}
      <div style={styles.filterRow}>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={styles.searchInput}
            placeholder="Search adjuster ID or period"
            value={state.search}
            onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
          />
        </div>
        <div style={styles.search}>
          <input
            style={{ ...styles.searchInput, width: 160 }}
            placeholder="Filter period (e.g. Q1-2024)"
            value={state.period}
            onChange={e => dispatch({ type: 'SET_PERIOD', payload: e.target.value })}
            onKeyDown={e => { if (e.key === 'Enter') load(); }}
          />
          <Trophy size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
        </div>
      </div>

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.tierFilter !== 'ALL' || state.search ? 'No matching adjusters' : 'No adjuster records'}
            description={state.tierFilter !== 'ALL' || state.search ? 'Try clearing filters to view all records.' : 'Records will appear as performance data is captured.'}
            actions={(state.tierFilter !== 'ALL' || state.search) ? (
              <GhostButton onClick={() => { dispatch({ type: 'SET_TIER', payload: 'ALL' }); dispatch({ type: 'SET_SEARCH', payload: '' }); }}>
                Clear filters
              </GhostButton>
            ) : undefined}
          />
        ) : (
          <Table
            rowKey="perfId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            scroll={{ x: 900 }}
          />
        )}
      </DataCard>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiRow: {
    display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16,
  },
  filterRow: {
    display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap',
  },
  search: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    height: 28, padding: '0 10px',
    border: '1px solid var(--ci-border-strong)',
    borderRadius: 'var(--ci-radius-input)',
    background: 'var(--ci-bg-surface)',
  },
  searchInput: {
    border: 'none', outline: 'none', background: 'transparent',
    fontSize: 11, color: 'var(--ci-text-primary)',
    width: 200,
  },
  slaBarTrack: {
    flex: 1, height: 6, borderRadius: 3,
    background: 'var(--ci-bg-surface-2)', overflow: 'hidden',
    maxWidth: 120,
  },
  slaBarFill: {
    height: '100%', borderRadius: 3, transition: 'width 0.3s',
  },
};

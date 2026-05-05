import { useEffect, useMemo, useReducer, useCallback, useState } from 'react';
import {
  Table, Button, Alert, Tooltip, Popconfirm, Typography, Dropdown,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { RefreshCw, Search, Trash2, X } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
  PageHeader, DataCard, GhostButton, EmptyState, Badge, Chip,
} from '../../components/ui';
import {
  ingestApi,
  type RawClaim,
} from '../../api/dataIngestionApi';
import { useAuth } from '../../hooks/useAuth';
import { canEdit } from '../../utils/roles';

const { Text } = Typography;

/**
 * RawClaimsPage — admin utility view of ingested claim payloads.
 * No dedicated reference screen; follows the same admin pattern as FeedsPage.
 */

/** Time-window options driving the "Recent" filter chip. null = no limit. */
type WindowKey = '24h' | '7d' | '30d' | 'all';
const TIME_WINDOWS: { key: WindowKey; label: string; hours: number | null }[] = [
  { key: '24h', label: 'Last 24 hours', hours: 24      },
  { key: '7d',  label: 'Last 7 days',   hours: 24 * 7  },
  { key: '30d', label: 'Last 30 days',  hours: 24 * 30 },
  { key: 'all', label: 'All time',      hours: null    },
];

interface State {
  items: RawClaim[];
  loading: boolean;
  error: string | null;
  searchClaimId: string;
  windowKey: WindowKey;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: RawClaim[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'DELETE'; payload: number }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_WINDOW'; payload: WindowKey };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':      return { ...s, loading: true, error: null };
    case 'SUCCESS':    return { ...s, loading: false, items: a.payload };
    case 'ERROR':      return { ...s, loading: false, error: a.payload };
    case 'DELETE':     return { ...s, items: s.items.filter(i => i.rawId !== a.payload) };
    case 'SET_SEARCH': return { ...s, searchClaimId: a.payload };
    case 'SET_WINDOW': return { ...s, windowKey: a.payload };
    default:           return s;
  }
}

export default function RawClaimsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { role } = useAuth();
  // Fraud Analysts can read raw payloads for investigation but cannot delete
  const canWrite = canEdit(role, '/ingestion/raw');

  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null,
    searchClaimId: '', windowKey: 'all',
  });

  // ?feedId=N — set when navigating here from the Data Feeds "Claims" count link.
  const [feedFilter, setFeedFilter] = useState<number | null>(() => {
    const p = searchParams.get('feedId');
    return p ? Number(p) : null;
  });

  const activeWindow = TIME_WINDOWS.find(w => w.key === state.windowKey) ?? TIME_WINDOWS[3];

  // Apply time-window + optional feed filter client-side.
  // Newest first so the most recent ingest is always on top.
  const filtered = useMemo(() => {
    let list = activeWindow.hours === null
      ? state.items
      : state.items.filter(item => {
          if (!item.ingestedDate) return false;
          const ageMs = Date.now() - new Date(item.ingestedDate).getTime();
          if (Number.isNaN(ageMs)) return false;
          return ageMs <= activeWindow.hours! * 3_600_000;
        });
    if (feedFilter !== null) list = list.filter(i => i.feedId === feedFilter);
    return [...list].sort((a, b) =>
      (b.ingestedDate ?? '').localeCompare(a.ingestedDate ?? '')
    );
  }, [state.items, activeWindow.hours, feedFilter]);

  const windowMenu: MenuProps = {
    items: TIME_WINDOWS.map(w => ({ key: w.key, label: w.label })),
    selectable: true,
    selectedKeys: [state.windowKey],
    onClick: ({ key }) => dispatch({ type: 'SET_WINDOW', payload: key as WindowKey }),
  };

  const load = useCallback(async (claimId?: string) => {
    dispatch({ type: 'START' });
    try {
      const data = claimId?.trim()
        ? await ingestApi.getByClaim(claimId.trim())
        : await ingestApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to load raw claims.';
      dispatch({ type: 'ERROR', payload: msg });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id: number) => {
    try {
      await ingestApi.delete(id);
      dispatch({ type: 'DELETE', payload: id });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to delete raw claim.';
      dispatch({ type: 'ERROR', payload: msg });
    }
  };

  const columns: ColumnsType<RawClaim> = [
    {
      title: 'Raw ID',
      dataIndex: 'rawId',
      key: 'rawId',
      width: 90,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim ID',
      dataIndex: 'claimId',
      key: 'claimId',
      width: '20%',
      render: (v: string) => (
        <a onClick={() => navigate(`/claims/${v}`)} style={{ fontWeight: 500, color: 'var(--ci-primary)' }}>
          {v}
        </a>
      ),
    },
    {
      title: 'Source feed',
      key: 'feed',
      width: 160,
      render: (_, rec) => (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <Badge tone="blue">{rec.feedType ?? '—'}</Badge>
          <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>
            #{rec.feedId}
          </span>
        </span>
      ),
    },
    {
      title: 'Payload',
      dataIndex: 'payloadJson',
      key: 'payloadJson',
      ellipsis: true,
      render: v => <Text style={{ fontSize: 11, color: 'var(--ci-text-muted)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' }}>{v}</Text>,
    },
    {
      title: 'Ingested',
      dataIndex: 'ingestedDate',
      key: 'ingestedDate',
      width: 130,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v?.replace('T', ' ').slice(0, 16) ?? '—'}</span>,
    },
    {
      title: '',
      key: 'action',
      width: 60,
      align: 'right',
      render: (_, rec) => canWrite ? (
        <Popconfirm title="Delete this raw claim?" onConfirm={() => handleDelete(rec.rawId)}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ) : null,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Raw claims"
        subtitle={
          feedFilter !== null
            ? `${filtered.length.toLocaleString()} payload${filtered.length === 1 ? '' : 's'} · feed #${feedFilter}`
            : activeWindow.hours === null
              ? `${state.items.length.toLocaleString()} ingested payload${state.items.length === 1 ? '' : 's'}`
              : `${filtered.length.toLocaleString()} of ${state.items.length.toLocaleString()} payloads · ${activeWindow.label.toLowerCase()}`
        }
        actions={
          <>
            {feedFilter !== null && (
              <Chip
                active
                onClick={() => setFeedFilter(null)}
              >
                Feed #{feedFilter} ✕
              </Chip>
            )}
            <div style={styles.search}>
              <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
              <input
                style={styles.searchInput}
                placeholder="Search claim ID"
                value={state.searchClaimId}
                onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
                onKeyDown={e => { if (e.key === 'Enter') load(state.searchClaimId); }}
              />
              {state.searchClaimId && (
                <X
                  size={12} strokeWidth={1.8} color="var(--ci-text-muted)"
                  style={{ cursor: 'pointer' }}
                  onClick={() => { dispatch({ type: 'SET_SEARCH', payload: '' }); load(); }}
                />
              )}
            </div>
            <Dropdown menu={windowMenu} trigger={['click']}>
              <Chip dropdown active={state.windowKey !== 'all'}>
                {activeWindow.label}
              </Chip>
            </Dropdown>
            <GhostButton onClick={() => load()} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
          </>
        }
      />

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 && !state.error ? (
          <EmptyState
            title={
              state.searchClaimId
                ? 'No matches'
                : activeWindow.hours !== null
                  ? `No claims ingested in the ${activeWindow.label.toLowerCase()}`
                  : 'No raw claims yet'
            }
            description={
              state.searchClaimId
                ? `No ingested payloads for claim "${state.searchClaimId}".`
                : activeWindow.hours !== null
                  ? state.items.length > 0
                    ? `${state.items.length} older payload${state.items.length === 1 ? '' : 's'} exist — switch to "All time" to see them.`
                    : 'Ingested payloads from your registered feeds will appear here.'
                  : 'Ingested payloads from your registered feeds will appear here.'
            }
            actions={
              state.searchClaimId
                ? <GhostButton onClick={() => { dispatch({ type: 'SET_SEARCH', payload: '' }); load(); }}>Clear search</GhostButton>
                : activeWindow.hours !== null && state.items.length > 0
                  ? <GhostButton onClick={() => dispatch({ type: 'SET_WINDOW', payload: 'all' })}>Show all time</GhostButton>
                  : undefined
            }
          />
        ) : (
          <Table
            rowKey="rawId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            size="small"
          />
        )}
      </DataCard>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
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
    width: 160,
  },
};

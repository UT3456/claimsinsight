import { useEffect, useReducer, useCallback, useRef } from 'react';
import {
  Table, Alert, DatePicker, Typography, Dropdown, Modal, Button, Tooltip, Popconfirm,
  App as AntApp,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { RefreshCw, Search, Trash2, ChevronDown } from 'lucide-react';
import dayjs from 'dayjs';

import {
  PageHeader, DataCard, Badge, GhostButton, EmptyState,
} from '../../components/ui';
import type { BadgeTone } from '../../components/ui';
import { auditApi, type AuditLog, type AuditPage } from '../../api/auditApi';
import { canEdit } from '../../utils/roles';
import { useAuth } from '../../hooks/useAuth';

const { Text } = Typography;
const { RangePicker } = DatePicker;

/**
 * AuditLogsPage — admin activity trail.
 * No dedicated reference screen; uses the admin table pattern (PageHeader +
 * DataCard wrapping a compact table, semantic Badge per action).
 */

interface State {
  items: AuditLog[];
  totalElements: number;
  page: number;
  loading: boolean;
  error: string | null;
  searchUser: string;
  searchAction: string;
  dateRange: [string, string] | null;
  mode: 'all' | 'user' | 'action' | 'date';
}

type Action =
  | { type: 'START' }
  | { type: 'PAGE'; payload: AuditPage }
  | { type: 'LIST'; payload: AuditLog[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'SET_PAGE'; payload: number }
  | { type: 'SET_USER'; payload: string }
  | { type: 'SET_ACTION'; payload: string }
  | { type: 'SET_DATE'; payload: [string, string] | null }
  | { type: 'SET_MODE'; payload: State['mode'] };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':     return { ...s, loading: true, error: null };
    case 'PAGE':      return {
      ...s, loading: false,
      items: a.payload.content,
      totalElements: a.payload.totalElements,
      page: a.payload.number,
    };
    case 'LIST':      return { ...s, loading: false, items: a.payload, totalElements: a.payload.length };
    case 'ERROR':     return { ...s, loading: false, error: a.payload };
    case 'SET_PAGE':  return { ...s, page: a.payload };
    case 'SET_USER':  return { ...s, searchUser: a.payload };
    case 'SET_ACTION':return { ...s, searchAction: a.payload };
    case 'SET_DATE':  return { ...s, dateRange: a.payload };
    case 'SET_MODE':  return { ...s, mode: a.payload };
    default:          return s;
  }
}

function actionTone(action: string): BadgeTone {
  if (!action) return 'neutral';
  const a = action.toUpperCase();
  if (a.startsWith('CREATE'))   return 'green';
  if (a.startsWith('VIEW'))     return 'blue';
  if (a.startsWith('UPDATE'))   return 'amber';
  if (a.startsWith('DELETE'))   return 'red';
  if (a === 'LOGIN')            return 'purple';
  if (a === 'REGISTER')         return 'teal';
  if (a.startsWith('MARK'))     return 'blue';
  if (a === 'LOGOUT')           return 'amber';
  if (a.startsWith('VALIDATE')) return 'green';
  return 'neutral';
}

export default function AuditLogsPage() {
  const { role } = useAuth();
  // Only admins can clear audit logs — fraud analysts have read-only access
  const canClear = canEdit(role, '/admin/audit-logs');
  const { message, modal } = AntApp.useApp();
  const [state, dispatch] = useReducer(reducer, {
    items: [], totalElements: 0, page: 0,
    loading: false, error: null,
    searchUser: '', searchAction: '', dateRange: null, mode: 'all',
  });

  // Latest filter values, read at request time so we don't refetch on every keystroke.
  const latest = useRef({
    mode: state.mode, page: state.page,
    user: state.searchUser, action: state.searchAction, date: state.dateRange,
  });
  latest.current = {
    mode: state.mode, page: state.page,
    user: state.searchUser, action: state.searchAction, date: state.dateRange,
  };

  // Guards against React StrictMode double-mounting (which would otherwise
  // trigger two GETs, and since /audit/logs records its own access the
  // second call sees a fresh "VIEW" row from the first — making entries
  // appear duplicated).
  const didInitialLoad = useRef(false);

  const load = useCallback(async () => {
    const { mode, page, user, action, date } = latest.current;
    dispatch({ type: 'START' });
    try {
      if (mode === 'user' && user) {
        dispatch({ type: 'LIST', payload: await auditApi.getByUser(user) });
      } else if (mode === 'action' && action) {
        dispatch({ type: 'LIST', payload: await auditApi.getByAction(action) });
      } else if (mode === 'date' && date) {
        dispatch({ type: 'LIST', payload: await auditApi.getByDateRange(date[0], date[1]) });
      } else {
        dispatch({ type: 'PAGE', payload: await auditApi.getLogs(page, 20) });
      }
    } catch {
      dispatch({ type: 'ERROR', payload: 'Failed to load audit logs.' });
    }
  }, []);

  // Initial mount only.
  useEffect(() => {
    if (didInitialLoad.current) return;
    didInitialLoad.current = true;
    load();
  }, [load]);

  // Auto-refetch only when committed inputs change — mode (Enter pressed),
  // page (pagination), or dateRange (picker committed). NOT search-input typing.
  useEffect(() => {
    if (!didInitialLoad.current) return;
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.mode, state.page, state.dateRange]);

  const handleReset = () => {
    dispatch({ type: 'SET_USER', payload: '' });
    dispatch({ type: 'SET_ACTION', payload: '' });
    dispatch({ type: 'SET_DATE', payload: null });
    dispatch({ type: 'SET_PAGE', payload: 0 });
    dispatch({ type: 'SET_MODE', payload: 'all' });
  };

  // ── Clear handlers (admin-only) ────────────────────────────────────────────

  const handleClearAll = () => {
    modal.confirm({
      title: 'Clear ALL audit logs?',
      content:
        `This will permanently delete every audit record (${state.totalElements.toLocaleString()} entries). ` +
        `A single AUDIT_CLEARED marker will be left behind so the action itself is recorded. ` +
        `This cannot be undone.`,
      okText: 'Delete all',
      okButtonProps: { danger: true },
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          const result = await auditApi.clearAll();
          message.success(`Cleared ${result.removed.toLocaleString()} audit record${result.removed === 1 ? '' : 's'}`);
          load();
        } catch (err) {
          const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to clear audit logs.';
          message.error(msg);
        }
      },
    });
  };

  const handlePurgeOlder = (days: number) => {
    const cutoff = dayjs().subtract(days, 'day').format('YYYY-MM-DDTHH:mm:ss');
    modal.confirm({
      title: `Purge audit logs older than ${days} day${days === 1 ? '' : 's'}?`,
      content:
        `Removes every record before ${cutoff.replace('T', ' ')}. ` +
        `Recent activity is preserved. An AUDIT_PURGED marker is added with the row count.`,
      okText: 'Purge',
      okButtonProps: { danger: true },
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          const result = await auditApi.clearOlderThan(cutoff);
          message.success(`Purged ${result.removed.toLocaleString()} record${result.removed === 1 ? '' : 's'} older than ${days} day${days === 1 ? '' : 's'}`);
          load();
        } catch (err) {
          const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to purge audit logs.';
          message.error(msg);
        }
      },
    });
  };

  const handleDeleteRow = async (id: number) => {
    try {
      await auditApi.deleteById(id);
      message.success(`Audit record #${id} deleted`);
      load();
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to delete record.';
      message.error(msg);
    }
  };

  const clearMenu: MenuProps = {
    items: [
      { key: '1d',  label: 'Older than 1 day' },
      { key: '7d',  label: 'Older than 7 days' },
      { key: '30d', label: 'Older than 30 days' },
      { type: 'divider' },
      { key: 'all', label: 'Clear all', danger: true },
    ],
    onClick: ({ key }) => {
      if (key === 'all')      handleClearAll();
      else if (key === '1d')  handlePurgeOlder(1);
      else if (key === '7d')  handlePurgeOlder(7);
      else if (key === '30d') handlePurgeOlder(30);
    },
  };

  const columns: ColumnsType<AuditLog> = [
    {
      title: 'ID', dataIndex: 'id', key: 'id', width: 70,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      // Visually merge consecutive rows for the same user so we don't render
      // "utkarsh / utkarsh / utkarsh / utkarsh / utkarsh" five times in a row.
      // The first row of each group gets rowSpan=N (where N is the run length);
      // subsequent rows get rowSpan=0, which Ant Design hides automatically.
      // The merged cell aligns to the top so it's clear it covers the whole run.
      title: 'User', dataIndex: 'username', key: 'username', width: 160,
      onCell: (_rec, index) => {
        if (index === undefined) return {};
        const items = state.items;
        const current = items[index]?.username;
        const prev    = items[index - 1]?.username;
        // Subsequent row in a same-user run → hidden by rowSpan: 0
        if (prev !== undefined && prev === current) {
          return { rowSpan: 0 };
        }
        // Count how many consecutive following rows belong to the same user
        let span = 1;
        for (let i = index + 1; i < items.length; i++) {
          if (items[i].username === current) span++;
          else break;
        }
        return {
          rowSpan: span,
          style: { verticalAlign: 'top' as const },
        };
      },
      render: v => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Action', dataIndex: 'action', key: 'action', width: 140,
      render: (v: string) => <Badge tone={actionTone(v)}>{v}</Badge>,
    },
    {
      title: 'Endpoint', dataIndex: 'endpoint', key: 'endpoint',
      ellipsis: true,
      render: v => (
        <Text style={{ fontSize: 11, fontFamily: 'ui-monospace, SFMono-Regular, monospace', color: 'var(--ci-text-muted)' }}>{v}</Text>
      ),
    },
    {
      title: 'Details', dataIndex: 'details', key: 'details',
      ellipsis: true,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span>,
    },
    {
      // No client-side sorter — data is already returned newest-first by the
      // backend, and a re-sort would scramble the consecutive same-user runs
      // that the User column visually merges.
      title: 'Timestamp', dataIndex: 'timestamp', key: 'timestamp', width: 160,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v?.replace('T', ' ').slice(0, 19)}</span>,
    },
    ...(canClear ? [{
      title: '',
      key: 'action',
      width: 50,
      align: 'right' as const,
      render: (_: unknown, rec: AuditLog) => (
        <Popconfirm
          title="Delete this audit record?"
          description="This action cannot be undone."
          onConfirm={() => handleDeleteRow(rec.id)}
          okText="Delete"
          okButtonProps={{ danger: true }}
        >
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    }] : []),
  ];

  return (
    <div>
      <PageHeader
        title="Audit log"
        subtitle={`${state.totalElements.toLocaleString()} record${state.totalElements === 1 ? '' : 's'} · full action trail`}
        actions={
          <>
            <GhostButton onClick={handleReset} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Reset
            </GhostButton>
            {canClear && (
              <Dropdown menu={clearMenu} trigger={['click']}>
                <Button danger size="small" icon={<Trash2 size={12} strokeWidth={1.8} />}>
                  Clear <ChevronDown size={11} strokeWidth={1.8} />
                </Button>
              </Dropdown>
            )}
          </>
        }
      />

      {/* Filter rail */}
      <div style={styles.filterRow}>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={styles.searchInput}
            placeholder="Username"
            value={state.searchUser}
            onChange={e => dispatch({ type: 'SET_USER', payload: e.target.value })}
            onKeyDown={e => {
              if (e.key !== 'Enter') return;
              if (state.searchUser) {
                dispatch({ type: 'SET_MODE', payload: 'user' });
                if (state.mode === 'user') load();   // mode unchanged → trigger manually
              } else {
                dispatch({ type: 'SET_MODE', payload: 'all' });
              }
            }}
          />
        </div>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={{ ...styles.searchInput, width: 200 }}
            placeholder="Action (CREATE, DELETE…)"
            value={state.searchAction}
            onChange={e => dispatch({ type: 'SET_ACTION', payload: e.target.value })}
            onKeyDown={e => {
              if (e.key !== 'Enter') return;
              if (state.searchAction) {
                dispatch({ type: 'SET_MODE', payload: 'action' });
                if (state.mode === 'action') load();   // mode unchanged → trigger manually
              } else {
                dispatch({ type: 'SET_MODE', payload: 'all' });
              }
            }}
          />
        </div>
        <RangePicker
          showTime size="small"
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              dispatch({
                type: 'SET_DATE',
                payload: [dates[0].format('YYYY-MM-DDTHH:mm:ss'), dates[1].format('YYYY-MM-DDTHH:mm:ss')] as [string, string],
              });
              dispatch({ type: 'SET_MODE', payload: 'date' });
            } else {
              dispatch({ type: 'SET_DATE', payload: null });
              dispatch({ type: 'SET_MODE', payload: 'all' });
            }
          }}
          style={{ borderRadius: 'var(--ci-radius-input)' }}
        />
      </div>

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      <DataCard padding={0}>
        {!state.loading && state.items.length === 0 && !state.error ? (
          <EmptyState
            title="No audit records"
            description="Activity will appear here as users interact with the platform."
            tone="positive"
          />
        ) : (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={state.items}
            loading={state.loading}
            size="small"
            pagination={
              state.mode === 'all'
                ? {
                    current: state.page + 1,
                    pageSize: 20,
                    total: state.totalElements,
                    size: 'small',
                    onChange: p => dispatch({ type: 'SET_PAGE', payload: p - 1 }),
                  }
                : { pageSize: 20, size: 'small' }
            }
          />
        )}
      </DataCard>

      <div style={styles.hint}>
        Press <kbd style={styles.kbd}>Enter</kbd> in a search field to filter.
      </div>
    </div>
  );
}

// Suppress unused import warning (dayjs type used via RangePicker onChange)
void dayjs;

const styles: Record<string, React.CSSProperties> = {
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
    width: 160,
  },
  hint: {
    marginTop: 8, fontSize: 10, color: 'var(--ci-text-muted)',
  },
  kbd: {
    background: 'var(--ci-bg-surface-2)',
    border: '1px solid var(--ci-border)',
    borderRadius: 3, padding: '0 4px',
    fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 10,
  },
};

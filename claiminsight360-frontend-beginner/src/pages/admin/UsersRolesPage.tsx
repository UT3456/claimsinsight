import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { Table, Alert, Tooltip, Button, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { MoreHorizontal, Plus, Search } from 'lucide-react';

import {
  PageHeader, DataCard, Badge, DarkButton, EmptyState, TintedAvatar,
} from '../../components/ui';
import type { BadgeTone } from '../../components/ui';
import { auditApi, type AuditLog } from '../../api/auditApi';
import { ROLE_LABELS, ROLES, type AppRole } from '../../utils/roles';

const { Text } = Typography;

/**
 * UsersRolesPage — reference screen #13 "Users & roles".
 *
 * We don't yet have a dedicated /users API, so the table is assembled from
 * the most-recent audit log entries (one row per distinct username). The
 * role column shows an educated guess based on username heuristics where
 * the audit log doesn't expose role directly. When the users API ships,
 * swap the data source — the table shape stays the same.
 */

interface UserRow {
  username: string;
  email: string;
  role: AppRole;
  status: 'Active' | 'Invited' | 'Suspended';
  lastActive: string;
}

// Persona-based role tones matching the reference.
const ROLE_TONE: Record<AppRole, BadgeTone> = {
  [ROLES.CLAIMS_ANALYST]:   'teal',
  [ROLES.CLAIMS_MANAGER]:   'blue',
  [ROLES.FRAUD_ANALYST]:    'red',
  [ROLES.ACTUARY]:          'purple',
  [ROLES.OPERATIONS_EXEC]:  'amber',
  [ROLES.ADMIN]:            'neutral',
};

const STATUS_TONE: Record<UserRow['status'], BadgeTone> = {
  Active:    'green',
  Invited:   'amber',
  Suspended: 'red',
};

// ── Heuristics to derive email + role from a username ────────────────────────

function guessEmail(username: string): string {
  const slug = username.toLowerCase().replace(/[^a-z0-9.]/g, '.');
  return `${slug}@acme.com`;
}

function guessRole(username: string): AppRole {
  const u = username.toLowerCase();
  if (u.includes('admin'))   return ROLES.ADMIN;
  if (u.includes('fraud'))   return ROLES.FRAUD_ANALYST;
  if (u.includes('actuary')) return ROLES.ACTUARY;
  if (u.includes('ops') || u.includes('exec')) return ROLES.OPERATIONS_EXEC;
  if (u.includes('manager') || u.includes('mgr')) return ROLES.CLAIMS_MANAGER;
  return ROLES.CLAIMS_ANALYST;
}

function relativeActive(iso: string | undefined): string {
  if (!iso) return '—';
  const then = new Date(iso).getTime();
  if (isNaN(then)) return '—';
  const diffMin = Math.max(0, Math.floor((Date.now() - then) / 60000));
  if (diffMin < 2)          return 'now';
  if (diffMin < 60)         return `${diffMin}m`;
  if (diffMin < 60 * 24)    return `${Math.floor(diffMin / 60)}h`;
  return `${Math.floor(diffMin / (60 * 24))}d`;
}

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  users: UserRow[];
  loading: boolean;
  error: string | null;
  search: string;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: UserRow[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'SEARCH'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':   return { ...s, loading: true, error: null };
    case 'SUCCESS': return { ...s, loading: false, users: a.payload };
    case 'ERROR':   return { ...s, loading: false, error: a.payload };
    case 'SEARCH':  return { ...s, search: a.payload };
    default:        return s;
  }
}

export default function UsersRolesPage() {
  const [state, dispatch] = useReducer(reducer, {
    users: [], loading: false, error: null, search: '',
  });

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const page = await auditApi.getLogs(0, 200);
      const logs: AuditLog[] = page.content ?? [];
      const byUser = new Map<string, AuditLog>();
      for (const log of logs) {
        if (!log.username) continue;
        const prev = byUser.get(log.username);
        if (!prev || prev.timestamp < log.timestamp) byUser.set(log.username, log);
      }
      const users: UserRow[] = Array.from(byUser.values()).map(log => ({
        username:   log.username,
        email:      guessEmail(log.username),
        role:       guessRole(log.username),
        status:     'Active',
        lastActive: relativeActive(log.timestamp),
      }));
      dispatch({ type: 'SUCCESS', payload: users });
    } catch {
      dispatch({ type: 'ERROR', payload: 'User directory unavailable. Wire up a /users endpoint to enable management.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(() => {
    const q = state.search.trim().toLowerCase();
    if (!q) return state.users;
    return state.users.filter(u =>
      u.username.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q) ||
      ROLE_LABELS[u.role].toLowerCase().includes(q),
    );
  }, [state.users, state.search]);

  const columns: ColumnsType<UserRow> = [
    {
      title: 'User',
      dataIndex: 'username',
      key: 'user',
      width: '28%',
      render: (username: string) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <TintedAvatar name={username} colorKey={username} size={26} />
          <span style={{ fontWeight: 500 }}>{username}</span>
        </div>
      ),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      width: '24%',
      render: v => <Text style={{ color: 'var(--ci-text-secondary)', fontSize: 11 }}>{v}</Text>,
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      width: '20%',
      render: (role: AppRole) => <Badge tone={ROLE_TONE[role]}>{ROLE_LABELS[role]}</Badge>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: '14%',
      render: (v: UserRow['status']) => <Badge tone={STATUS_TONE[v]}>{v}</Badge>,
    },
    {
      title: 'Last active',
      dataIndex: 'lastActive',
      key: 'lastActive',
      width: '10%',
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>{v}</span>,
    },
    {
      title: '',
      key: 'action',
      width: '4%',
      align: 'right',
      render: () => (
        <Tooltip title="Actions (coming soon)">
          <Button
            type="text" size="small" disabled
            icon={<MoreHorizontal size={14} strokeWidth={1.6} />}
            style={{ color: 'var(--ci-text-muted)' }}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Users & roles"
        subtitle={`${state.users.length} active user${state.users.length === 1 ? '' : 's'} · RBAC`}
        actions={
          <>
            <div style={styles.search}>
              <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
              <input
                style={styles.searchInput}
                placeholder="Search users"
                value={state.search}
                onChange={e => dispatch({ type: 'SEARCH', payload: e.target.value })}
              />
            </div>
            <DarkButton icon={<Plus size={12} strokeWidth={2} />} disabled>
              Invite user
            </DarkButton>
          </>
        }
      />

      {state.error && (
        <Alert
          type="info" showIcon
          message="Directory is read-only"
          description={state.error}
          style={{ marginBottom: 12, borderRadius: 8 }}
          closable
        />
      )}

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.search ? 'No matches' : 'No users yet'}
            description={
              state.search
                ? `No users matching “${state.search}”.`
                : 'Users will appear here as they sign in and generate audit events.'
            }
          />
        ) : (
          <Table
            rowKey="username"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
          />
        )}
      </DataCard>

      <div style={styles.footnote}>
        Users are derived from recent audit events. A dedicated <code style={styles.code}>/users</code> API will
        enable invite and role-edit actions.
      </div>
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
  footnote: {
    marginTop: 12, fontSize: 11,
    color: 'var(--ci-text-muted)', lineHeight: 1.5,
  },
  code: {
    fontFamily: 'ui-monospace, SFMono-Regular, monospace',
    background: 'var(--ci-bg-surface-2)',
    padding: '1px 5px',
    borderRadius: 3, border: '1px solid var(--ci-border)',
    fontSize: 10,
  },
};

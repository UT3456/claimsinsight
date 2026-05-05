import { useEffect, useMemo, useReducer, useCallback } from 'react';
import {
  Table, Alert, Tooltip, Button, Typography, Dropdown, Popconfirm, Switch,
  App as AntApp,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { MoreHorizontal, Search, RefreshCw, Trash2 } from 'lucide-react';

import {
  PageHeader, DataCard, Badge, GhostButton, EmptyState, TintedAvatar,
} from '../../components/ui';
import type { BadgeTone } from '../../components/ui';
import { usersApi, type User } from '../../api/usersApi';
import { ROLE_LABELS, ROLES, type AppRole } from '../../utils/roles';
import { useAuth } from '../../hooks/useAuth';

const { Text } = Typography;

/**
 * UsersRolesPage — backed by the gateway's /api/users endpoints.
 * Admins can: list, change role, enable/disable, and delete users.
 * Self-deletion is blocked server-side AND in the UI.
 */

const ROLE_TONE: Record<AppRole, BadgeTone> = {
  [ROLES.CLAIMS_ANALYST]:   'teal',
  [ROLES.CLAIMS_MANAGER]:   'blue',
  [ROLES.FRAUD_ANALYST]:    'red',
  [ROLES.ACTUARY]:          'purple',
  [ROLES.OPERATIONS_EXEC]:  'amber',
  [ROLES.ADMIN]:            'neutral',
};

interface State {
  users: User[];
  loading: boolean;
  error: string | null;
  search: string;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: User[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'SEARCH'; payload: string }
  | { type: 'PATCH'; payload: User }
  | { type: 'REMOVE'; payload: number };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':   return { ...s, loading: true, error: null };
    case 'SUCCESS': return { ...s, loading: false, users: a.payload };
    case 'ERROR':   return { ...s, loading: false, error: a.payload };
    case 'SEARCH':  return { ...s, search: a.payload };
    case 'PATCH':   return { ...s, users: s.users.map(u => u.id === a.payload.id ? a.payload : u) };
    case 'REMOVE':  return { ...s, users: s.users.filter(u => u.id !== a.payload) };
    default:        return s;
  }
}

export default function UsersRolesPage() {
  const { username: currentUsername } = useAuth();
  const { message } = AntApp.useApp();
  const [state, dispatch] = useReducer(reducer, {
    users: [], loading: false, error: null, search: '',
  });

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const data = await usersApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to load users.';
      dispatch({ type: 'ERROR', payload: msg });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleRoleChange = async (user: User, role: AppRole) => {
    try {
      const updated = await usersApi.update(user.id, { role });
      dispatch({ type: 'PATCH', payload: updated });
      message.success(`Role updated to ${ROLE_LABELS[role]}`);
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to update role.';
      message.error(msg);
    }
  };

  const handleToggleEnabled = async (user: User, enabled: boolean) => {
    try {
      const updated = await usersApi.update(user.id, { enabled });
      dispatch({ type: 'PATCH', payload: updated });
      message.success(enabled ? 'User enabled' : 'User disabled — sessions revoked');
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to update status.';
      message.error(msg);
    }
  };

  const handleDelete = async (user: User) => {
    try {
      await usersApi.delete(user.id);
      dispatch({ type: 'REMOVE', payload: user.id });
      message.success(`User '${user.username}' deleted`);
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to delete user.';
      message.error(msg);
    }
  };

  const filtered = useMemo(() => {
    const q = state.search.trim().toLowerCase();
    if (!q) return state.users;
    return state.users.filter(u =>
      u.username.toLowerCase().includes(q) ||
      (u.email ?? '').toLowerCase().includes(q) ||
      (u.name ?? '').toLowerCase().includes(q) ||
      ROLE_LABELS[u.role].toLowerCase().includes(q),
    );
  }, [state.users, state.search]);

  const columns: ColumnsType<User> = [
    {
      title: 'User',
      dataIndex: 'username',
      key: 'user',
      width: '24%',
      render: (username: string, rec) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <TintedAvatar name={username} colorKey={username} size={26} />
          <div>
            <div style={{ fontWeight: 500 }}>{username}</div>
            {rec.name && <div style={{ fontSize: 10, color: 'var(--ci-text-muted)' }}>{rec.name}</div>}
          </div>
        </div>
      ),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      width: '22%',
      render: v => <Text style={{ color: 'var(--ci-text-secondary)', fontSize: 11 }}>{v}</Text>,
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      width: '20%',
      render: (role: AppRole, rec) => {
        const isSelf = rec.username === currentUsername;
        const items: MenuProps['items'] = Object.entries(ROLE_LABELS).map(([key, label]) => ({
          key,
          label,
          disabled: key === role,
        }));
        const menuProps: MenuProps = {
          items,
          onClick: ({ key }) => handleRoleChange(rec, key as AppRole),
        };
        return isSelf ? (
          <Badge tone={ROLE_TONE[role]}>{ROLE_LABELS[role]}</Badge>
        ) : (
          <Dropdown menu={menuProps} trigger={['click']}>
            <span style={{ cursor: 'pointer' }}>
              <Badge tone={ROLE_TONE[role]}>{ROLE_LABELS[role]} ▾</Badge>
            </span>
          </Dropdown>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'enabled',
      key: 'enabled',
      width: '14%',
      render: (enabled: boolean, rec) => {
        const isSelf = rec.username === currentUsername;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Switch
              size="small"
              checked={enabled}
              disabled={isSelf}
              onChange={(checked) => handleToggleEnabled(rec, checked)}
            />
            <span style={{ fontSize: 11, color: enabled ? 'var(--ci-text-secondary)' : 'var(--ci-text-muted)' }}>
              {enabled ? 'Active' : 'Disabled'}
            </span>
          </div>
        );
      },
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: '14%',
      render: v => <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>{v?.replace('T', ' ').slice(0, 16) ?? '—'}</span>,
    },
    {
      title: '',
      key: 'action',
      width: '6%',
      align: 'right',
      render: (_, rec) => {
        const isSelf = rec.username === currentUsername;
        if (isSelf) {
          return (
            <Tooltip title="You cannot delete your own account">
              <Button type="text" size="small" disabled icon={<MoreHorizontal size={14} strokeWidth={1.6} />} />
            </Tooltip>
          );
        }
        return (
          <Popconfirm
            title={`Delete user '${rec.username}'?`}
            description="This action cannot be undone. All refresh tokens will be revoked."
            onConfirm={() => handleDelete(rec)}
            okText="Delete"
            okButtonProps={{ danger: true }}
          >
            <Tooltip title="Delete">
              <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
            </Tooltip>
          </Popconfirm>
        );
      },
    },
  ];

  return (
    <div>
      <PageHeader
        title="Users & roles"
        subtitle={`${state.users.length} user${state.users.length === 1 ? '' : 's'} · RBAC`}
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
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
          </>
        }
      />

      {state.error && (
        <Alert
          type="error" showIcon
          message={state.error}
          style={{ marginBottom: 12, borderRadius: 8 }}
          closable
        />
      )}

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.search ? 'No matches' : 'No users'}
            description={
              state.search
                ? `No users matching “${state.search}”.`
                : 'Users will appear here as they register through the auth endpoint.'
            }
          />
        ) : (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
          />
        )}
      </DataCard>

      <div style={styles.footnote}>
        Click a role badge to change it. Toggling status revokes all active refresh tokens.
        You cannot edit or delete your own account.
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
};

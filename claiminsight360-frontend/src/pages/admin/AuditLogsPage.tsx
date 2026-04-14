import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Input, Typography, Space, Tag, Alert,
  Card, Row, Col, Statistic, DatePicker,
} from 'antd';
import { ReloadOutlined, SearchOutlined, AuditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { auditApi, AuditLog, AuditPage } from '../../api/auditApi';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: AuditLog[];
  totalElements: number;
  totalPages: number;
  page: number;
  loading: boolean;
  error: string | null;
  searchUser: string;
  searchAction: string;
  dateRange: [string, string] | null;
  mode: 'all' | 'user' | 'action' | 'date';
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: AuditPage }
  | { type: 'FETCH_LIST'; payload: AuditLog[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'SET_PAGE'; payload: number }
  | { type: 'SET_USER'; payload: string }
  | { type: 'SET_ACTION'; payload: string }
  | { type: 'SET_DATE_RANGE'; payload: [string, string] | null }
  | { type: 'SET_MODE'; payload: State['mode'] };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':   return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS': return {
      ...state, loading: false,
      items: action.payload.content,
      totalElements: action.payload.totalElements,
      totalPages: action.payload.totalPages,
      page: action.payload.number,
    };
    case 'FETCH_LIST':    return { ...state, loading: false, items: action.payload, totalElements: action.payload.length };
    case 'FETCH_ERROR':   return { ...state, loading: false, error: action.payload };
    case 'SET_PAGE':      return { ...state, page: action.payload };
    case 'SET_USER':      return { ...state, searchUser: action.payload };
    case 'SET_ACTION':    return { ...state, searchAction: action.payload };
    case 'SET_DATE_RANGE':return { ...state, dateRange: action.payload };
    case 'SET_MODE':      return { ...state, mode: action.payload };
    default:              return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

function getActionColor(action: string): string {
  if (!action) return 'default';
  const a = action.toUpperCase();
  if (a.startsWith('CREATE'))   return 'green';
  if (a.startsWith('VIEW'))     return 'blue';
  if (a.startsWith('UPDATE'))   return 'orange';
  if (a.startsWith('DELETE'))   return 'red';
  if (a === 'LOGIN')            return 'purple';
  if (a === 'REGISTER')         return 'cyan';
  if (a.startsWith('MARK'))     return 'geekblue';
  if (a === 'LOGOUT')           return 'volcano';
  if (a.startsWith('VALIDATE')) return 'lime';
  return 'default';
}

export default function AuditLogsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], totalElements: 0, totalPages: 0, page: 0,
    loading: false, error: null,
    searchUser: '', searchAction: '', dateRange: null, mode: 'all',
  });

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      if (state.mode === 'user' && state.searchUser) {
        const data = await auditApi.getByUser(state.searchUser);
        dispatch({ type: 'FETCH_LIST', payload: data });
      } else if (state.mode === 'action' && state.searchAction) {
        const data = await auditApi.getByAction(state.searchAction);
        dispatch({ type: 'FETCH_LIST', payload: data });
      } else if (state.mode === 'date' && state.dateRange) {
        const data = await auditApi.getByDateRange(state.dateRange[0], state.dateRange[1]);
        dispatch({ type: 'FETCH_LIST', payload: data });
      } else {
        const page = await auditApi.getLogs(state.page, 20);
        dispatch({ type: 'FETCH_SUCCESS', payload: page });
      }
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load audit logs' });
    }
  }, [state.mode, state.page, state.searchUser, state.searchAction, state.dateRange]);

  useEffect(() => { load(); }, [load]);

  const handleUserSearch = () => {
    dispatch({ type: 'SET_MODE', payload: 'user' });
  };

  const handleActionSearch = () => {
    dispatch({ type: 'SET_MODE', payload: 'action' });
  };

  const handleDateSearch = (dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
    if (dates) {
      dispatch({ type: 'SET_DATE_RANGE', payload: [dates[0].format('YYYY-MM-DDTHH:mm:ss'), dates[1].format('YYYY-MM-DDTHH:mm:ss')] });
      dispatch({ type: 'SET_MODE', payload: 'date' });
    } else {
      dispatch({ type: 'SET_DATE_RANGE', payload: null });
      dispatch({ type: 'SET_MODE', payload: 'all' });
    }
  };

  const handleReset = () => {
    dispatch({ type: 'SET_MODE', payload: 'all' });
    dispatch({ type: 'SET_USER', payload: '' });
    dispatch({ type: 'SET_ACTION', payload: '' });
    dispatch({ type: 'SET_DATE_RANGE', payload: null });
    dispatch({ type: 'SET_PAGE', payload: 0 });
  };

  // Derive stats from current page
  const uniqueUsers   = new Set(state.items.map(i => i.username)).size;
  const createCount   = state.items.filter(i => i.action?.toUpperCase().startsWith('CREATE')).length;
  const deleteCount   = state.items.filter(i => i.action?.toUpperCase().startsWith('DELETE')).length;

  const columns: ColumnsType<AuditLog> = [
    { title: 'ID',       dataIndex: 'id',       key: 'id',       width: 80 },
    { title: 'Username', dataIndex: 'username', key: 'username', render: v => <Text code>{v}</Text> },
    {
      title: 'Action', dataIndex: 'action', key: 'action',
      render: (v: string) => <Tag color={getActionColor(v)}>{v}</Tag>,
      filters: ['CREATE', 'VIEW', 'UPDATE', 'DELETE', 'LOGIN', 'REGISTER', 'LOGOUT'].map(a => ({ text: a, value: a })),
      onFilter: (value, rec) => rec.action?.toUpperCase().startsWith(String(value)),
    },
    { title: 'Endpoint', dataIndex: 'endpoint', key: 'endpoint', ellipsis: true, render: v => <Text type="secondary">{v}</Text> },
    { title: 'Details',  dataIndex: 'details',  key: 'details',  ellipsis: true },
    {
      title: 'Timestamp', dataIndex: 'timestamp', key: 'timestamp',
      sorter: (a, b) => a.timestamp.localeCompare(b.timestamp),
      render: v => <Text>{v?.replace('T', ' ').slice(0, 19)}</Text>,
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Audit Logs</Title>
          <Text type="secondary">Complete trail of all user actions</Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>Reset &amp; Refresh</Button>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Showing Records',  value: state.totalElements, color: '#2563eb' },
          { title: 'Unique Users',     value: uniqueUsers,          color: '#7c3aed' },
          { title: 'Create Actions',   value: createCount,          color: '#16a34a' },
          { title: 'Delete Actions',   value: deleteCount,          color: '#dc2626' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value}
                valueStyle={{ color: c.color }} prefix={<AuditOutlined />} />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Search bar */}
      <Card style={{ marginBottom: 16, borderRadius: 10 }}>
        <Space wrap>
          <Input
            placeholder="Search by username"
            value={state.searchUser}
            style={{ width: 200 }}
            onChange={e => dispatch({ type: 'SET_USER', payload: e.target.value })}
            onPressEnter={handleUserSearch}
            suffix={<SearchOutlined onClick={handleUserSearch} style={{ cursor: 'pointer' }} />}
          />
          <Input
            placeholder="Search by action (CREATE, DELETE…)"
            value={state.searchAction}
            style={{ width: 220 }}
            onChange={e => dispatch({ type: 'SET_ACTION', payload: e.target.value })}
            onPressEnter={handleActionSearch}
            suffix={<SearchOutlined onClick={handleActionSearch} style={{ cursor: 'pointer' }} />}
          />
          <RangePicker
            showTime
            onChange={(dates) => handleDateSearch(dates as [dayjs.Dayjs, dayjs.Dayjs] | null)}
          />
        </Space>
      </Card>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        bordered size="middle"
        pagination={
          state.mode === 'all'
            ? {
                current: state.page + 1,
                pageSize: 20,
                total: state.totalElements,
                onChange: p => dispatch({ type: 'SET_PAGE', payload: p - 1 }),
              }
            : { pageSize: 20 }
        }
      />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header:   { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24, flexWrap: 'wrap', gap: 12 },
  statCard: { borderRadius: 10, border: '1px solid #e2e8f0' },
};

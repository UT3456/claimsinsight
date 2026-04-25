import { useReducer, useEffect } from 'react';
import {
  Card, Row, Col, Typography, Tag, Statistic, Avatar,
  Descriptions, Alert, Spin, Divider,
} from 'antd';
import {
  UserOutlined, SafetyCertificateOutlined,
  ClockCircleOutlined, KeyOutlined,
} from '@ant-design/icons';
import { FiCheckCircle, FiLock } from 'react-icons/fi';
import { useAuth } from '../../hooks/useAuth';
import { notificationsApi } from '../../api/notificationsApi';
import { ROLE_LABELS, ROLE_COLORS, type AppRole } from '../../utils/roles';
import { decodeToken } from '../../utils/tokenUtils';
import { useSelector } from 'react-redux';
import { selectToken } from '../../store/slices/authSlice';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  unreadCount: number;
  totalNotifications: number;
  loading: boolean;
  error: string | null;
  tokenExpiry: string | null;
  issuedAt: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: { unread: number; total: number } }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'SET_TOKEN_INFO'; payload: { expiry: string; issuedAt: string } };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':    return { ...state, loading: false, unreadCount: action.payload.unread, totalNotifications: action.payload.total };
    case 'FETCH_ERROR':      return { ...state, loading: false, error: action.payload };
    case 'SET_TOKEN_INFO':   return { ...state, tokenExpiry: action.payload.expiry, issuedAt: action.payload.issuedAt };
    default:                 return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function ProfilePage() {
  const { username, role, userId } = useAuth();
  const token = useSelector(selectToken);

  const [state, dispatch] = useReducer(reducer, {
    unreadCount: 0, totalNotifications: 0,
    loading: false, error: null,
    tokenExpiry: null, issuedAt: null,
  });

  useEffect(() => {
    // Decode token for expiry info
    if (token) {
      const payload = decodeToken(token);
      if (payload) {
        dispatch({
          type: 'SET_TOKEN_INFO',
          payload: {
            expiry:   dayjs.unix(payload.exp).format('YYYY-MM-DD HH:mm:ss'),
            issuedAt: dayjs.unix(payload.iat).format('YYYY-MM-DD HH:mm:ss'),
          },
        });
      }
    }

    // Fetch notification stats
    if (!userId) return;
    dispatch({ type: 'FETCH_START' });
    Promise.all([
      notificationsApi.getUnreadCount(userId).catch(() => 0),
      notificationsApi.getForUser(userId).catch(() => []),
    ]).then(([unread, all]) => {
      dispatch({
        type: 'FETCH_SUCCESS',
        payload: {
          unread: typeof unread === 'number' ? unread : 0,
          total:  Array.isArray(all) ? all.length : 0,
        },
      });
    }).catch(() => {
      dispatch({ type: 'FETCH_ERROR', payload: 'Could not load notification stats' });
    });
  }, [userId, token]);

  const roleLabel = role ? (ROLE_LABELS[role as AppRole] ?? role) : '';
  const roleColor = role ? (ROLE_COLORS[role as AppRole] ?? 'default') : 'default';

  const initials = username
    ? username.slice(0, 2).toUpperCase()
    : 'U';

  return (
    <div>
      <div style={styles.header}>
        <Title level={3} style={{ margin: 0 }}>My Profile</Title>
        <Text type="secondary">Account details and session information</Text>
      </div>

      {state.error && (
        <Alert type="warning" message={state.error} style={{ marginBottom: 16 }} closable />
      )}

      <Row gutter={[20, 20]}>
        {/* Left — identity card */}
        <Col xs={24} md={8}>
          <Card style={styles.card}>
            <div style={styles.avatarSection}>
              <Avatar
                size={96}
                style={{ background: '#185FA5', fontSize: 32, fontWeight: 500 }}
              >
                {initials}
              </Avatar>
              <Title level={4} style={{ margin: '16px 0 4px' }}>{username}</Title>
              <Tag color={roleColor} style={{ fontSize: 13, padding: '2px 12px' }}>{roleLabel}</Tag>
            </div>

            <Divider />

            <Descriptions column={1} size="small" labelStyle={{ color: '#64748b', fontWeight: 500 }}>
              <Descriptions.Item label={<><UserOutlined /> &nbsp;User ID</>}>
                <Text code>#{userId ?? '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label={<><SafetyCertificateOutlined /> &nbsp;Role</>}>
                <Text>{roleLabel || '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label={<><KeyOutlined /> &nbsp;Session issued</>}>
                <Text>{state.issuedAt ?? '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label={<><ClockCircleOutlined /> &nbsp;Session expires</>}>
                <Text>{state.tokenExpiry ?? '—'}</Text>
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        {/* Right — stats + permissions */}
        <Col xs={24} md={16}>
          {/* Notification stats */}
          <Card title="Notification Summary" style={{ ...styles.card, marginBottom: 20 }}>
            {state.loading ? (
              <div style={{ textAlign: 'center', padding: 24 }}><Spin /></div>
            ) : (
              <Row gutter={16}>
                <Col xs={12} sm={8}>
                  <Statistic title="Total Notifications" value={state.totalNotifications}
                    valueStyle={{ color: '#2563eb' }} />
                </Col>
                <Col xs={12} sm={8}>
                  <Statistic title="Unread" value={state.unreadCount}
                    valueStyle={{ color: state.unreadCount > 0 ? '#dc2626' : '#16a34a' }} />
                </Col>
                <Col xs={12} sm={8}>
                  <Statistic title="Read" value={state.totalNotifications - state.unreadCount}
                    valueStyle={{ color: '#16a34a' }} />
                </Col>
              </Row>
            )}
          </Card>

          {/* Access permissions */}
          <Card title="Access Permissions" style={styles.card}>
            <Row gutter={[8, 8]}>
              {[
                { label: 'Claims Analytics',    routes: ['/claims', '/ingestion/feeds', '/ingestion/raw'],  roles: ['ROLE_CLAIMS_ANALYST', 'ROLE_CLAIMS_MANAGER', 'ROLE_ACTUARY', 'ROLE_OPERATIONS_EXEC', 'ROLE_ADMIN'] },
                { label: 'Adjuster Operations', routes: ['/adjusters', '/sla-violations'],                  roles: ['ROLE_CLAIMS_MANAGER', 'ROLE_OPERATIONS_EXEC', 'ROLE_ADMIN'] },
                { label: 'Financial',           routes: ['/costs', '/reserves', '/aging'],                  roles: ['ROLE_ACTUARY', 'ROLE_CLAIMS_MANAGER', 'ROLE_ADMIN'] },
                { label: 'Fraud Risk',          routes: ['/fraud-risk'],                                    roles: ['ROLE_FRAUD_ANALYST', 'ROLE_ADMIN'] },
                { label: 'Denial & Leakage',    routes: ['/denial-leakage'],                               roles: ['ROLE_CLAIMS_ANALYST', 'ROLE_FRAUD_ANALYST', 'ROLE_ADMIN'] },
                { label: 'Reports',             routes: ['/reports'],                                       roles: ['ROLE_CLAIMS_ANALYST', 'ROLE_CLAIMS_MANAGER', 'ROLE_FRAUD_ANALYST', 'ROLE_ACTUARY', 'ROLE_OPERATIONS_EXEC', 'ROLE_ADMIN'] },
                { label: 'Notifications',       routes: ['/notifications'],                                 roles: ['ROLE_CLAIMS_ANALYST', 'ROLE_CLAIMS_MANAGER', 'ROLE_FRAUD_ANALYST', 'ROLE_ACTUARY', 'ROLE_OPERATIONS_EXEC', 'ROLE_ADMIN'] },
                { label: 'Audit Logs',          routes: ['/admin/audit-logs'],                             roles: ['ROLE_ADMIN'] },
              ].map(({ label, roles }) => {
                const hasAccess = role ? roles.includes(role) : false;
                return (
                  <Col xs={24} sm={12} key={label}>
                    <div style={{ ...styles.permRow, background: hasAccess ? 'rgba(60,179,113,0.08)' : 'rgba(244,91,105,0.08)', border: `1px solid ${hasAccess ? 'rgba(60,179,113,0.35)' : 'rgba(244,91,105,0.35)'}` }}>
                      <Text style={{ fontSize: 13, color: hasAccess ? '#3CB371' : '#F45B69', display: 'inline-flex', alignItems: 'center', gap: 8 }}>
                        {hasAccess ? <FiCheckCircle /> : <FiLock />} {label}
                      </Text>
                    </div>
                  </Col>
                );
              })}
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header:        { marginBottom: 24 },
  card:          { borderRadius: 12, border: '1px solid #e2e8f0' },
  avatarSection: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '8px 0 4px' },
  permRow:       { padding: '8px 14px', borderRadius: 8 },
};

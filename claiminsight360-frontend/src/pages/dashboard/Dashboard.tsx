import { useEffect, useReducer, useCallback } from 'react';
import { Card, Col, Row, Tag, Typography, Statistic, Alert, Spin } from 'antd';
import {
  FileTextOutlined, AlertOutlined, DollarOutlined, SafetyOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Button } from 'antd';
import { useAuth } from '../../hooks/useAuth';
import { riskScoresApi } from '../../api/fraudRiskApi';
import { reservesApi } from '../../api/financialApi';
import { slaApi } from '../../api/adjustersApi';
import { ingestApi } from '../../api/dataIngestionApi';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  totalIngested: number;
  activeSlaViolations: number;
  totalReserve: number;
  highRiskClaims: number;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: Omit<State, 'loading' | 'error'> }
  | { type: 'FETCH_ERROR'; payload: string };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':   return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS': return { ...state, loading: false, ...action.payload };
    case 'FETCH_ERROR':   return { ...state, loading: false, error: action.payload };
    default:              return state;
  }
}

const initial: State = {
  totalIngested: 0, activeSlaViolations: 0,
  totalReserve: 0, highRiskClaims: 0,
  loading: false, error: null,
};

// ── Component ─────────────────────────────────────────────────────────────────

export default function Dashboard() {
  const { username, roleLabel, roleColor } = useAuth();
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const [rawClaims, slaViolations, totalReserve, riskScores] = await Promise.allSettled([
        ingestApi.getAll(),
        slaApi.getAll(),
        reservesApi.getTotalAmount(),
        riskScoresApi.getAll(),
      ]);

      dispatch({
        type: 'FETCH_SUCCESS',
        payload: {
          totalIngested:       rawClaims.status      === 'fulfilled' ? rawClaims.value.length        : 0,
          activeSlaViolations: slaViolations.status  === 'fulfilled' ? slaViolations.value.length    : 0,
          totalReserve:        totalReserve.status   === 'fulfilled'
            ? (isNaN(Number(totalReserve.value)) ? 0 : Number(totalReserve.value))
            : 0,
          highRiskClaims:      riskScores.status     === 'fulfilled'
            ? riskScores.value.filter(s => s.scoreValue >= 75).length
            : 0,
        },
      });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load dashboard data' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const statCards = [
    {
      title:  'Total Claims Ingested',
      value:  state.loading ? '—' : state.totalIngested,
      icon:   <FileTextOutlined style={{ fontSize: 24, color: '#2563eb' }} />,
      color:  '#eff6ff',
      border: '#bfdbfe',
      valueColor: '#2563eb',
    },
    {
      title:  'Active SLA Violations',
      value:  state.loading ? '—' : state.activeSlaViolations,
      icon:   <AlertOutlined style={{ fontSize: 24, color: '#dc2626' }} />,
      color:  '#fef2f2',
      border: '#fecaca',
      valueColor: '#dc2626',
    },
    {
      title:   'Total Reserve Amount',
      value:   state.loading ? '—' : `$${Number(state.totalReserve).toLocaleString('en-US', { minimumFractionDigits: 0 })}`,
      icon:    <DollarOutlined style={{ fontSize: 24, color: '#16a34a' }} />,
      color:   '#f0fdf4',
      border:  '#bbf7d0',
      valueColor: '#16a34a',
    },
    {
      title:  'High-Risk Claims (≥75)',
      value:  state.loading ? '—' : state.highRiskClaims,
      icon:   <SafetyOutlined style={{ fontSize: 24, color: '#d97706' }} />,
      color:  '#fffbeb',
      border: '#fde68a',
      valueColor: '#d97706',
    },
  ];

  return (
    <div>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
            Welcome back, <span style={{ color: '#2563eb' }}>{username}</span> 👋
          </Title>
          <Text style={{ color: '#64748b' }}>
            Here&apos;s what&apos;s happening across your claims portfolio today.
          </Text>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Tag color={roleColor} style={{ fontSize: 13, padding: '4px 12px' }}>
            {roleLabel}
          </Tag>
          <Button icon={<ReloadOutlined />} onClick={load} loading={state.loading} size="small">
            Refresh
          </Button>
        </div>
      </div>

      {state.error && (
        <Alert type="warning" showIcon message={state.error}
          description="Some services may be offline. Showing available data."
          style={{ marginBottom: 24, borderRadius: 10 }} closable />
      )}

      {/* Stat cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card) => (
          <Col xs={24} sm={12} xl={6} key={card.title}>
            <Card
              style={{ borderRadius: 12, border: `1px solid ${card.border}`, background: card.color }}
              bodyStyle={{ padding: '20px 24px' }}
            >
              <div style={styles.statRow}>
                <Statistic
                  title={<span style={{ color: '#475569', fontSize: 13 }}>{card.title}</span>}
                  value={state.loading ? '—' : card.value}
                  valueStyle={{ color: card.valueColor, fontSize: 28, fontWeight: 700 }}
                  formatter={v => state.loading ? <Spin size="small" /> : String(v)}
                />
                <div style={styles.iconWrap}>{card.icon}</div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Summary panels */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card
            title="Platform Overview"
            style={styles.chartCard}
            headStyle={{ borderBottom: '1px solid #f1f5f9', fontWeight: 600 }}
            extra={<Tag color="blue">Live</Tag>}
          >
            <Row gutter={[16, 16]}>
              {[
                { label: 'Raw Claims in System',     value: state.totalIngested,       color: '#2563eb' },
                { label: 'SLA Violations Logged',    value: state.activeSlaViolations, color: '#dc2626' },
                { label: 'High-Risk Flags (Score≥75)',value: state.highRiskClaims,      color: '#d97706' },
                { label: 'Total Reserve ($)',
                  value: `$${Number(state.totalReserve).toLocaleString('en-US', { maximumFractionDigits: 0 })}`,
                  color: '#16a34a', isText: true },
              ].map(c => (
                <Col xs={12} key={c.label}>
                  <div style={styles.metricBox}>
                    {state.loading
                      ? <Spin size="small" />
                      : <Text strong style={{ fontSize: 22, color: c.color }}>
                          {c.isText ? c.value : c.value}
                        </Text>
                    }
                    <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                      {c.label}
                    </Text>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card
            title="Quick Navigation"
            style={styles.chartCard}
            headStyle={{ borderBottom: '1px solid #f1f5f9', fontWeight: 600 }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                { label: '📊 Claims KPI Dashboard',      path: '/claims',          color: '#eff6ff', border: '#bfdbfe' },
                { label: '⚠️  SLA Violations',             path: '/sla-violations',  color: '#fef2f2', border: '#fecaca' },
                { label: '🛡️ Fraud Risk Intelligence',    path: '/fraud-risk',      color: '#fdf4ff', border: '#e9d5ff' },
                { label: '💰 Cost & Reserve Analysis',    path: '/costs',           color: '#f0fdf4', border: '#bbf7d0' },
                { label: '📋 Analytics Reports',          path: '/reports',         color: '#fffbeb', border: '#fde68a' },
              ].map(({ label, path, color, border }) => (
                <a key={path} href={path} style={{ textDecoration: 'none' }}>
                  <div style={{ ...styles.navItem, background: color, border: `1px solid ${border}` }}>
                    <Text style={{ fontSize: 13 }}>{label}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>→</Text>
                  </div>
                </a>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex', alignItems: 'flex-start',
    justifyContent: 'space-between', flexWrap: 'wrap',
    gap: 12, marginBottom: 24,
  },
  statRow:   { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' },
  iconWrap:  {
    width: 48, height: 48, borderRadius: 12,
    background: 'rgba(255,255,255,0.8)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
  },
  chartCard: { borderRadius: 12, border: '1px solid #e2e8f0' },
  metricBox: {
    background: '#f8fafc', borderRadius: 10,
    padding: '16px', border: '1px solid #e2e8f0',
    display: 'flex', flexDirection: 'column',
  },
  navItem: {
    padding: '10px 14px', borderRadius: 8,
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    cursor: 'pointer',
  },
};

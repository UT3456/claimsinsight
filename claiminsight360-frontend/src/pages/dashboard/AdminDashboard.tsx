import { Button, Card, Col, Row } from 'antd';
import PageHeader  from '../../components/common/PageHeader';
import StatCard    from '../../components/common/StatCard';
import AlertBanner from '../../components/common/AlertBanner';
import { adminMock } from '../../mocks/dashboards';

const STATUS_TAG: Record<string, string> = {
  green:  'tag-green',
  yellow: 'tag-yellow',
  red:    'tag-red',
};

export default function AdminDashboard() {
  const m = adminMock;
  return (
    <div>
      <PageHeader
        title="System overview"
        subtitle="Platform health and activity"
        right={<Button type="primary" size="small">Settings</Button>}
      />

      <AlertBanner tone="red" style={{ marginBottom: 14 }}>{m.alertText}</AlertBanner>

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="Active users"    value={m.activeUsers.value}    sub={m.activeUsers.sub}    tone="muted" /></Col>
        <Col xs={12} sm={6}><StatCard label="Feeds healthy"   value={m.feedsHealthy.value}   sub={m.feedsHealthy.sub}   tone="down"  /></Col>
        <Col xs={12} sm={6}><StatCard label="Pending invites" value={m.pendingInvites.value} sub={m.pendingInvites.sub} tone="muted" /></Col>
        <Col xs={12} sm={6}><StatCard label="Uptime (7d)"     value={m.uptime.value}         sub={m.uptime.sub}         tone="up"    /></Col>
      </Row>

      <Row gutter={10}>
        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Data feed status</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: 0 }}
          >
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr>
                  <th style={th}>Feed</th>
                  <th style={th}>Sync</th>
                  <th style={th}>Status</th>
                </tr>
              </thead>
              <tbody>
                {m.feedStatus.map(f => (
                  <tr key={f.feed}>
                    <td style={td}>{f.feed}</td>
                    <td style={td}>{f.sync}</td>
                    <td style={td}><span className={STATUS_TAG[f.tone]}>{f.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Recent audit events</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            <div style={{ fontSize: 11, lineHeight: 1.8 }}>
              {m.auditEvents.map((e, i) => (
                <div key={i}>
                  <b>{e.time}</b> · {e.text}
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

const th: React.CSSProperties = {
  background: '#f7f7f7', padding: '7px 10px', textAlign: 'left',
  borderBottom: '1px solid var(--ci-border)', fontWeight: 600, fontSize: 11, color: '#555',
};
const td: React.CSSProperties = {
  padding: '7px 10px', borderBottom: '1px solid #eee', color: '#333', fontSize: 12,
};

import { Button, Card, Col, Row } from 'antd';
import PageHeader   from '../../components/common/PageHeader';
import StatCard     from '../../components/common/StatCard';
import ProgressBar  from '../../components/common/ProgressBar';
import AlertBanner  from '../../components/common/AlertBanner';
import SimpleBar    from '../../components/common/SimpleBar';
import { managerMock } from '../../mocks/dashboards';

export default function ManagerDashboard() {
  const m = managerMock;
  const barData = m.workload.map(w => ({ name: w.name, value: w.count, color: w.color }));

  return (
    <div>
      <PageHeader
        title="Team overview"
        subtitle="42 adjusters · Q1 2026"
        right={<Button type="primary" size="small">Manage workload</Button>}
      />

      <AlertBanner tone="red" style={{ marginBottom: 14 }}>{m.alertText}</AlertBanner>

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="Open claims"    value={m.openClaims.value}   sub={m.openClaims.sub}   tone="up"   /></Col>
        <Col xs={12} sm={6}><StatCard label="Avg team TAT"   value={m.avgTeamTat.value}   sub={m.avgTeamTat.sub}   tone="down" /></Col>
        <Col xs={12} sm={6}><StatCard label="SLA breaches"   value={m.slaBreaches.value}  sub={m.slaBreaches.sub}  tone="down" /></Col>
        <Col xs={12} sm={6}><StatCard label="Team quality"   value={m.teamQuality.value}  sub={m.teamQuality.sub}  tone="up"   /></Col>
      </Row>

      <Row gutter={10}>
        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Workload distribution</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            <SimpleBar data={barData} yTicks={['200', '100', '0']} />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Aging buckets</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            {m.aging.map(a => (
              <ProgressBar key={a.label} label={a.label} right={a.count.toLocaleString()} pct={a.pct} color={a.color} />
            ))}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

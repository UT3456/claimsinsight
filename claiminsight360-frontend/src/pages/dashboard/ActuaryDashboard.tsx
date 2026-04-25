import { Button, Card, Col, Row } from 'antd';
import PageHeader   from '../../components/common/PageHeader';
import StatCard     from '../../components/common/StatCard';
import ProgressBar  from '../../components/common/ProgressBar';
import SimpleLine   from '../../components/common/SimpleLine';
import { actuaryMock } from '../../mocks/dashboards';

export default function ActuaryDashboard() {
  const m = actuaryMock;
  const line = m.severityTrend.map(p => ({ label: p.m, value: p.v }));

  return (
    <div>
      <PageHeader
        title="Portfolio analytics"
        subtitle="Severity, reserves, loss ratio"
        right={<Button size="small">Export data</Button>}
      />

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="Loss ratio"   value={m.lossRatio.value}   sub={m.lossRatio.sub}   tone="up"   /></Col>
        <Col xs={12} sm={6}><StatCard label="Avg severity" value={m.avgSeverity.value} sub={m.avgSeverity.sub} tone="down" /></Col>
        <Col xs={12} sm={6}><StatCard label="Reserves"     value={m.reserves.value}    sub={m.reserves.sub}    tone="up"   /></Col>
        <Col xs={12} sm={6}><StatCard label="Frequency"    value={m.frequency.value}   sub={m.frequency.sub}   tone="muted" /></Col>
      </Row>

      <Row gutter={10}>
        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Severity trend (12 mo)</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            <SimpleLine data={line} />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Loss ratio by product</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            {m.lossRatioByProduct.map(p => (
              <ProgressBar key={p.product} label={p.product} right={p.value} pct={p.pct} color={p.color} />
            ))}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

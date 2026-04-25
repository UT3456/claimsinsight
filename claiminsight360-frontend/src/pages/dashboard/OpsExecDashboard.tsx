import { Button, Card, Col, Row } from 'antd';
import PageHeader  from '../../components/common/PageHeader';
import StatCard    from '../../components/common/StatCard';
import AlertBanner from '../../components/common/AlertBanner';
import SimpleBar   from '../../components/common/SimpleBar';
import { opsExecMock } from '../../mocks/dashboards';

export default function OpsExecDashboard() {
  const m = opsExecMock;
  const bar = m.monthlyCost.map(p => ({ name: p.m, value: p.v, color: '#2563eb' }));

  return (
    <div>
      <PageHeader
        title="Executive summary"
        subtitle="Organization-wide KPIs"
        right={<Button size="small">Monthly report</Button>}
      />

      <AlertBanner tone="green" style={{ marginBottom: 14 }}>{m.alertText}</AlertBanner>

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="Exposure"       value={m.exposure.value}      sub={m.exposure.sub}      tone="up" /></Col>
        <Col xs={12} sm={6}><StatCard label="Loss ratio"     value={m.lossRatio.value}     sub={m.lossRatio.sub}     tone="up" /></Col>
        <Col xs={12} sm={6}><StatCard label="SLA compliance" value={m.slaCompliance.value} sub={m.slaCompliance.sub} tone="up" /></Col>
        <Col xs={12} sm={6}><StatCard label="NPS"            value={m.nps.value}           sub={m.nps.sub}           tone="up" /></Col>
      </Row>

      <Card
        title={<span style={{ fontSize: 12, fontWeight: 600 }}>Monthly cost trend</span>}
        style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
        bodyStyle={{ padding: '12px 14px' }}
      >
        <SimpleBar data={bar} height={120} />
      </Card>
    </div>
  );
}

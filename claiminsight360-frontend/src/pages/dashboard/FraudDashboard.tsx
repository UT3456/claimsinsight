import { Button, Card, Col, Row } from 'antd';
import PageHeader  from '../../components/common/PageHeader';
import StatCard    from '../../components/common/StatCard';
import AlertBanner from '../../components/common/AlertBanner';
import SimpleBar   from '../../components/common/SimpleBar';
import { fraudMock } from '../../mocks/dashboards';

const SCORE_TAG: Record<'red' | 'yellow', string> = {
  red: 'tag-red', yellow: 'tag-yellow',
};

export default function FraudDashboard() {
  const m = fraudMock;
  const barData = m.scoreDistribution.map(d => ({ name: d.bucket, value: d.count, color: d.color }));

  return (
    <div>
      <PageHeader
        title="Risk command center"
        subtitle="342 high-risk claims need review"
        right={<Button type="primary" size="small">Review queue</Button>}
      />

      <AlertBanner tone="red" style={{ marginBottom: 14 }}>{m.alertText}</AlertBanner>

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="High-risk" value={m.highRisk.value} sub={m.highRisk.sub} variant="red"    tone="danger" /></Col>
        <Col xs={12} sm={6}><StatCard label="New flags" value={m.newFlags.value} sub={m.newFlags.sub} variant="yellow" tone="warn"   /></Col>
        <Col xs={12} sm={6}><StatCard label="Exposure"  value={m.exposure.value} sub={m.exposure.sub} tone="down" /></Col>
        <Col xs={12} sm={6}><StatCard label="Avg score" value={m.avgScore.value} sub={m.avgScore.sub} tone="muted" /></Col>
      </Row>

      <Card
        title={<span style={{ fontSize: 12, fontWeight: 600 }}>Risk score distribution</span>}
        style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none', marginBottom: 12 }}
        bodyStyle={{ padding: '12px 14px' }}
      >
        <SimpleBar data={barData} height={120} />
      </Card>

      <Card
        style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
        bodyStyle={{ padding: 0 }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr>
              <th style={th}>Claim</th>
              <th style={th}>Indicator</th>
              <th style={th}>Score</th>
              <th style={th}>Action</th>
            </tr>
          </thead>
          <tbody>
            {m.flagged.map(f => (
              <tr key={f.claim}>
                <td style={td}>{f.claim}</td>
                <td style={td}>{f.indicator}</td>
                <td style={td}><span className={SCORE_TAG[f.tone]}>{f.score}</span></td>
                <td style={td}><Button size="small">Review</Button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
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

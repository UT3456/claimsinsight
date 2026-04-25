import { Button, Card, Col, Row } from 'antd';
import PageHeader   from '../../components/common/PageHeader';
import StatCard     from '../../components/common/StatCard';
import ProgressBar  from '../../components/common/ProgressBar';
import { analystMock } from '../../mocks/dashboards';

const PRIORITY_TAG: Record<string, string> = {
  High: 'tag-red', Med: 'tag-yellow', Low: 'tag-green',
};

export default function AnalystDashboard() {
  const m = analystMock;
  return (
    <div>
      <PageHeader
        title="My claims overview"
        subtitle={`${m.assignedToMe.value} assigned · ${m.dueThisWeek.sub}`}
        right={<Button size="small">Filter</Button>}
      />

      <Row gutter={10} style={{ marginBottom: 14 }}>
        <Col xs={12} sm={6}><StatCard label="Assigned to me"  value={m.assignedToMe.value}  sub={m.assignedToMe.sub}  tone="muted" /></Col>
        <Col xs={12} sm={6}><StatCard label="Due this week"   value={m.dueThisWeek.value}   sub={m.dueThisWeek.sub}   tone="down"  /></Col>
        <Col xs={12} sm={6}><StatCard label="My avg TAT"      value={m.myAvgTat.value}      sub={m.myAvgTat.sub}      tone="up"    /></Col>
        <Col xs={12} sm={6}><StatCard label="Denials (week)"  value={m.denialsWeek.value}   sub={m.denialsWeek.sub}   tone="muted" /></Col>
      </Row>

      <Row gutter={10}>
        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Top denial codes (my claims)</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: '12px 14px' }}
          >
            {m.denialCodes.map(d => (
              <ProgressBar key={d.code} label={d.code} right={d.count} pct={d.pct} />
            ))}
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={<span style={{ fontSize: 12, fontWeight: 600 }}>Priority queue</span>}
            style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
            bodyStyle={{ padding: 0 }}
          >
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr>
                  <th style={th}>Claim</th>
                  <th style={th}>Age</th>
                  <th style={th}>Priority</th>
                </tr>
              </thead>
              <tbody>
                {m.priorityQueue.map(r => (
                  <tr key={r.claim}>
                    <td style={td}>{r.claim}</td>
                    <td style={td}>{r.age}</td>
                    <td style={td}><span className={PRIORITY_TAG[r.priority]}>{r.priority}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
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

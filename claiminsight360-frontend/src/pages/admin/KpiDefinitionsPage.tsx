import { useState } from 'react';
import { Button, Card, Col, Row, Modal, Form, Input, Switch } from 'antd';
import PageHeader from '../../components/common/PageHeader';
import { kpiDefinitionsMock, KpiDefinition } from '../../mocks/admin';

export default function KpiDefinitionsPage() {
  const [kpis, setKpis]   = useState<KpiDefinition[]>(kpiDefinitionsMock);
  const [open, setOpen]   = useState(false);
  const [form] = Form.useForm();

  const onSubmit = (vals: { name: string; formula: string; unit: string; note?: string; active: boolean }) => {
    setKpis(prev => [{ ...vals, note: vals.note ?? '', active: vals.active ?? true }, ...prev]);
    setOpen(false);
    form.resetFields();
  };

  return (
    <div>
      <PageHeader
        title="KPI Definitions"
        subtitle="Configure metrics"
        right={
          <Button type="primary" size="small" onClick={() => setOpen(true)}>+ New KPI</Button>
        }
      />

      <Row gutter={[10, 10]}>
        {kpis.map(k => (
          <Col xs={24} md={12} lg={8} key={k.name}>
            <Card
              style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
              bodyStyle={{ padding: 12 }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <b>{k.name}</b>
                <span className={k.active ? 'tag-green' : 'tag-yellow'}>
                  {k.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <div style={{
                background: '#f7f7f7', padding: '6px 8px', borderRadius: 3,
                fontFamily: 'monospace', fontSize: 11, marginBottom: 8,
              }}>
                {k.formula}
              </div>
              <div style={{ fontSize: 12, color: 'var(--ci-text-muted)' }}>
                Unit: {k.unit} · {k.note}
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Modal
        title="New KPI"
        open={open}
        onCancel={() => { setOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ active: true }}>
          <Form.Item name="name"    label="Name"    rules={[{ required: true }]}>
            <Input placeholder="e.g. Severity" />
          </Form.Item>
          <Form.Item name="formula" label="Formula" rules={[{ required: true }]}>
            <Input placeholder="avg(total_cost)" />
          </Form.Item>
          <Form.Item name="unit"    label="Unit"    rules={[{ required: true }]}>
            <Input placeholder="% / days / ₹" />
          </Form.Item>
          <Form.Item name="note"    label="Note">
            <Input placeholder="Target: ≤ 70" />
          </Form.Item>
          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

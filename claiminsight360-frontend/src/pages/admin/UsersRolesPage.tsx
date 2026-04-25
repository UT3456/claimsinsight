import { useState } from 'react';
import { Button, Card, Modal, Form, Input, Select } from 'antd';
import PageHeader from '../../components/common/PageHeader';
import { usersMock, AppUser } from '../../mocks/admin';

const ROLE_OPTIONS = [
  'Claims Analyst', 'Claims Manager', 'Fraud Analyst',
  'Actuary', 'Operations Exec', 'Admin',
];

const STATUS_TAG: Record<AppUser['status'], string> = {
  Active:    'tag-green',
  Invited:   'tag-yellow',
  Suspended: 'tag-red',
};

export default function UsersRolesPage() {
  const [users, setUsers] = useState<AppUser[]>(usersMock);
  const [open, setOpen]   = useState(false);
  const [form] = Form.useForm();

  const onSubmit = (vals: { name: string; email: string; role: string }) => {
    setUsers(prev => [
      { ...vals, status: 'Invited', lastActive: '—' },
      ...prev,
    ]);
    setOpen(false);
    form.resetFields();
  };

  const activeCount = users.filter(u => u.status === 'Active').length;

  return (
    <div>
      <PageHeader
        title="Users"
        subtitle={`${activeCount} active users`}
        right={<Button type="primary" size="small" onClick={() => setOpen(true)}>+ Invite user</Button>}
      />

      <Card
        style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
        bodyStyle={{ padding: 0 }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr>
              <th style={th}>Name</th>
              <th style={th}>Email</th>
              <th style={th}>Role</th>
              <th style={th}>Status</th>
              <th style={th}>Last active</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.email}>
                <td style={td}>{u.name}</td>
                <td style={td}>{u.email}</td>
                <td style={td}><span className="tag-blue">{u.role}</span></td>
                <td style={td}><span className={STATUS_TAG[u.status]}>{u.status}</span></td>
                <td style={td}>{u.lastActive}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Modal
        title="Invite User"
        open={open}
        onCancel={() => { setOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onSubmit}>
          <Form.Item name="name"  label="Name"  rules={[{ required: true }]}>
            <Input placeholder="Full name" />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
            <Input placeholder="user@acme.com" />
          </Form.Item>
          <Form.Item name="role"  label="Role"  rules={[{ required: true }]}>
            <Select
              placeholder="Select role"
              options={ROLE_OPTIONS.map(r => ({ label: r, value: r }))}
            />
          </Form.Item>
        </Form>
      </Modal>
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

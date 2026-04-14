import { useState }            from 'react';
import { useNavigate, Link }   from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver }         from '@hookform/resolvers/zod';
import { z }                   from 'zod';
import {
  Button, Form, Input, Alert, Typography, Select, Divider,
} from 'antd';
import {
  UserOutlined, LockOutlined, MailOutlined,
  SafetyCertificateOutlined, IdcardOutlined,
} from '@ant-design/icons';
import { authApi } from '../../api/authApi';

const { Title, Text } = Typography;

// ── Schema ───────────────────────────────────────────────────────────────────
const schema = z.object({
  username:        z.string().min(3, 'Username must be at least 3 characters'),
  email:           z.string().email('Enter a valid email address'),
  password:        z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  role:            z.string().min(1, 'Please select a role'),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path:    ['confirmPassword'],
});

type RegisterFormValues = z.infer<typeof schema>;

// ── Component ─────────────────────────────────────────────────────────────────
export default function RegisterPage() {
  const navigate = useNavigate();
  const [loading,  setLoading]  = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [success,  setSuccess]  = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver:      zodResolver(schema),
    defaultValues: { username: '', email: '', password: '', confirmPassword: '', role: '' },
  });

  const onSubmit = async (values: RegisterFormValues) => {
    setLoading(true);
    setApiError(null);
    try {
      await authApi.register({
        username: values.username,
        email:    values.email,
        password: values.password,
        role:     values.role,
      });
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Registration failed. Username or email may already be taken.';
      setApiError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.blobTop}    />
      <div style={styles.blobBottom} />

      <div style={styles.card}>
        {/* Header */}
        <div style={styles.logoRow}>
          <div style={styles.logoIcon}>
            <SafetyCertificateOutlined style={{ fontSize: 28, color: '#fff' }} />
          </div>
          <div>
            <Title level={4} style={{ margin: 0, color: '#0f172a', lineHeight: 1.2 }}>
              ClaimInsight<span style={{ color: '#2563eb' }}>360</span>
            </Title>
            <Text style={{ fontSize: 11, color: '#64748b', letterSpacing: '0.05em' }}>
              INSURANCE ANALYTICS PLATFORM
            </Text>
          </div>
        </div>

        <Divider style={{ margin: '20px 0' }} />

        <Title level={3} style={{ margin: '0 0 4px', color: '#0f172a' }}>
          Create account
        </Title>
        <Text style={{ color: '#64748b', display: 'block', marginBottom: 24 }}>
          Fill in the details below to get started
        </Text>

        {/* Feedback */}
        {apiError && (
          <Alert
            type="error" message={apiError} showIcon closable
            onClose={() => setApiError(null)}
            style={{ marginBottom: 20, borderRadius: 8 }}
          />
        )}
        {success && (
          <Alert
            type="success"
            message="Account created successfully! Redirecting to login…"
            showIcon
            style={{ marginBottom: 20, borderRadius: 8 }}
          />
        )}

        <Form layout="vertical" onFinish={handleSubmit(onSubmit)} size="large">

          {/* Username */}
          <Form.Item
            label={<span style={styles.label}>Username</span>}
            validateStatus={errors.username ? 'error' : ''}
            help={errors.username?.message}
          >
            <Controller name="username" control={control} render={({ field }) => (
              <Input {...field}
                prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
                placeholder="Choose a username"
                style={styles.input}
                autoFocus
              />
            )} />
          </Form.Item>

          {/* Email */}
          <Form.Item
            label={<span style={styles.label}>Email address</span>}
            validateStatus={errors.email ? 'error' : ''}
            help={errors.email?.message}
          >
            <Controller name="email" control={control} render={({ field }) => (
              <Input {...field}
                prefix={<MailOutlined style={{ color: '#94a3b8' }} />}
                placeholder="you@company.com"
                style={styles.input}
                type="email"
              />
            )} />
          </Form.Item>

          {/* Role */}
          <Form.Item
            label={<span style={styles.label}>Role</span>}
            validateStatus={errors.role ? 'error' : ''}
            help={errors.role?.message}
          >
            <Controller name="role" control={control} render={({ field }) => (
              <Select
                {...field}
                suffixIcon={<IdcardOutlined style={{ color: '#94a3b8' }} />}
                style={{ width: '100%', borderRadius: 8 }}
                placeholder="Select your role"
                options={[
                  {
                    label: 'Analytics & Operations',
                    options: [
                      { value: 'ROLE_CLAIMS_ANALYST',  label: '📊 Claims Analyst — Trends, denials, cost drivers' },
                      { value: 'ROLE_CLAIMS_MANAGER',  label: '🗂️ Claims Manager — Performance, workloads, cycle time' },
                      { value: 'ROLE_OPERATIONS_EXEC', label: '📈 Operations Executive — KPIs, TAT, SLA compliance' },
                    ],
                  },
                  {
                    label: 'Risk & Finance',
                    options: [
                      { value: 'ROLE_FRAUD_ANALYST',   label: '🔍 Fraud Analyst — Risk dashboards, red-flag indicators' },
                      { value: 'ROLE_ACTUARY',         label: '🧮 Actuary — Severity, frequency, pricing, reserving' },
                    ],
                  },
                  {
                    label: 'Administration',
                    options: [
                      { value: 'ROLE_ADMIN',           label: '🛡️ Admin — Full access, roles, configuration' },
                    ],
                  },
                ]}
              />
            )} />
          </Form.Item>

          {/* Password */}
          <Form.Item
            label={<span style={styles.label}>Password</span>}
            validateStatus={errors.password ? 'error' : ''}
            help={errors.password?.message}
          >
            <Controller name="password" control={control} render={({ field }) => (
              <Input.Password {...field}
                prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                placeholder="Min. 8 characters"
                style={styles.input}
                autoComplete="new-password"
              />
            )} />
          </Form.Item>

          {/* Confirm password */}
          <Form.Item
            label={<span style={styles.label}>Confirm password</span>}
            validateStatus={errors.confirmPassword ? 'error' : ''}
            help={errors.confirmPassword?.message}
            style={{ marginBottom: 28 }}
          >
            <Controller name="confirmPassword" control={control} render={({ field }) => (
              <Input.Password {...field}
                prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                placeholder="Repeat your password"
                style={styles.input}
                autoComplete="new-password"
              />
            )} />
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="primary" htmlType="submit"
              loading={loading} disabled={success} block
              style={styles.submitBtn}
            >
              {loading ? 'Creating account…' : 'Create Account'}
            </Button>
          </Form.Item>
        </Form>

        <div style={{ textAlign: 'center' }}>
          <Text style={{ color: '#64748b', fontSize: 14 }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: '#2563eb', fontWeight: 600 }}>
              Sign in
            </Link>
          </Text>
        </div>

        <Divider style={{ margin: '24px 0 16px' }} />
        <Text style={{ display: 'block', textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>
          Protected by enterprise-grade security · All sessions are audited
        </Text>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0f172a 100%)',
    padding: '24px 16px', position: 'relative', overflow: 'hidden',
  },
  blobTop: {
    position: 'absolute', top: '-120px', right: '-100px',
    width: '400px', height: '400px', borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(37,99,235,0.25) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  blobBottom: {
    position: 'absolute', bottom: '-150px', left: '-120px',
    width: '500px', height: '500px', borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  card: {
    position: 'relative', zIndex: 1, background: '#ffffff',
    borderRadius: '16px', padding: '40px 44px',
    width: '100%', maxWidth: '460px',
    boxShadow: '0 25px 60px rgba(0,0,0,0.35)',
  },
  logoRow:  { display: 'flex', alignItems: 'center', gap: '14px' },
  logoIcon: {
    width: '50px', height: '50px', borderRadius: '12px',
    background: 'linear-gradient(135deg, #1d4ed8, #2563eb)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    boxShadow: '0 4px 14px rgba(37,99,235,0.4)', flexShrink: 0,
  },
  label:     { color: '#374151', fontWeight: 600, fontSize: 14 },
  input:     { borderRadius: '8px', border: '1.5px solid #e2e8f0' },
  submitBtn: {
    height: '48px', borderRadius: '10px',
    background: 'linear-gradient(135deg, #1d4ed8, #2563eb)',
    border: 'none', fontSize: '16px', fontWeight: 600,
    letterSpacing: '0.02em', boxShadow: '0 4px 14px rgba(37,99,235,0.4)',
  },
};

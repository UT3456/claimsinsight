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
import {
  FiBarChart2, FiFolder, FiTrendingUp,
  FiSearch,
} from 'react-icons/fi';
import { TbAbacus } from 'react-icons/tb';

const roleOption = (icon: React.ReactNode, text: string) => (
  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
    <span style={{ color: '#2563eb', display: 'inline-flex', fontSize: 16 }}>{icon}</span>
    {text}
  </span>
);
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
    <div className="ci-auth-page">
      <div className="ci-auth-card" style={{ maxWidth: 400 }}>
        {/* Header */}
        <Title level={4} style={{ margin: '0 0 4px', color: '#222', textAlign: 'center' }}>
          Create account
        </Title>
        <Text style={{ color: '#888', display: 'block', marginBottom: 20, textAlign: 'center', fontSize: 12 }}>
          Get started with ClaimInsight360
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
                      { value: 'ROLE_CLAIMS_ANALYST',  label: roleOption(<FiBarChart2 />,  'Claims Analyst — Trends, denials, cost drivers') },
                      { value: 'ROLE_CLAIMS_MANAGER',  label: roleOption(<FiFolder />,     'Claims Manager — Performance, workloads, cycle time') },
                      { value: 'ROLE_OPERATIONS_EXEC', label: roleOption(<FiTrendingUp />, 'Operations Executive — KPIs, TAT, SLA compliance') },
                    ],
                  },
                  {
                    label: 'Risk & Finance',
                    options: [
                      { value: 'ROLE_FRAUD_ANALYST',   label: roleOption(<FiSearch />,     'Fraud Analyst — Risk dashboards, red-flag indicators') },
                      { value: 'ROLE_ACTUARY',         label: roleOption(<TbAbacus />,     'Actuary — Severity, frequency, pricing, reserving') },
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

        <Divider style={{ margin: '16px 0' }} />
        <div style={{ textAlign: 'center' }}>
          <Text style={{ color: '#888', fontSize: 12 }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: '#2563eb' }}>Sign in</Link>
          </Text>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  label:     { color: '#555', fontWeight: 600, fontSize: 13 },
  input:     { borderRadius: '4px', border: '1px solid #ccc' },
  submitBtn: {
    height: '38px', borderRadius: '4px',
    background: '#2563eb', border: 'none',
    fontSize: '14px', fontWeight: 600,
  },
};

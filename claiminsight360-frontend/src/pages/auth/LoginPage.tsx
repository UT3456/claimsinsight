import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useDispatch }          from 'react-redux';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import {
  Button,
  Form,
  Input,
  Alert,
  Typography,
  Checkbox,
  Divider,
} from 'antd';
import {
  UserOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';

import { authApi }         from '../../api/authApi';
import { setCredentials }  from '../../store/slices/authSlice';

const { Title, Text } = Typography;

// ── Validation schema ────────────────────────────────────────────────────────
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

// ── Component ────────────────────────────────────────────────────────────────
export default function LoginPage() {
  const navigate  = useNavigate();
  const dispatch  = useDispatch();
  const [loading, setLoading]   = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '', remember: false },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setLoading(true);
    setApiError(null);
    try {
      const response = await authApi.login({
        username: values.username,
        password: values.password,
      });
      dispatch(setCredentials(response));
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Invalid username or password. Please try again.';
      setApiError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      {/* ── Background blobs ── */}
      <div style={styles.blobTop}    />
      <div style={styles.blobBottom} />

      {/* ── Card ── */}
      <div style={styles.card}>
        {/* Logo + heading */}
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
          Welcome back
        </Title>
        <Text style={{ color: '#64748b', display: 'block', marginBottom: 24 }}>
          Sign in to your account to continue
        </Text>

        {/* API error */}
        {apiError && (
          <Alert
            type="error"
            message={apiError}
            showIcon
            closable
            onClose={() => setApiError(null)}
            style={{ marginBottom: 20, borderRadius: 8 }}
          />
        )}

        {/* Form */}
        <Form layout="vertical" onFinish={handleSubmit(onSubmit)} size="large">
          {/* Username */}
          <Form.Item
            label={<span style={styles.label}>Username</span>}
            validateStatus={errors.username ? 'error' : ''}
            help={errors.username?.message}
          >
            <Controller
              name="username"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
                  placeholder="Enter your username"
                  style={styles.input}
                  autoComplete="username"
                  autoFocus
                />
              )}
            />
          </Form.Item>

          {/* Password */}
          <Form.Item
            label={<span style={styles.label}>Password</span>}
            validateStatus={errors.password ? 'error' : ''}
            help={errors.password?.message}
            style={{ marginBottom: 8 }}
          >
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                  placeholder="Enter your password"
                  style={styles.input}
                  autoComplete="current-password"
                />
              )}
            />
          </Form.Item>

          {/* Remember me */}
          <Form.Item style={{ marginBottom: 24 }}>
            <Controller
              name="remember"
              control={control}
              render={({ field }) => (
                <Checkbox
                  checked={field.value}
                  onChange={(e) => field.onChange(e.target.checked)}
                >
                  <Text style={{ color: '#475569', fontSize: 14 }}>Remember me</Text>
                </Checkbox>
              )}
            />
          </Form.Item>

          {/* Submit */}
          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={styles.submitBtn}
            >
              {loading ? 'Signing in…' : 'Sign In'}
            </Button>
          </Form.Item>
        </Form>

        {/* Register link */}
        <div style={{ textAlign: 'center' }}>
          <Text style={{ color: '#64748b', fontSize: 14 }}>
            Don&apos;t have an account?{' '}
            <Link to="/register" style={{ color: '#2563eb', fontWeight: 600 }}>
              Create account
            </Link>
          </Text>
        </div>

        {/* Footer note */}
        <Divider style={{ margin: '24px 0 16px' }} />
        <Text
          style={{ display: 'block', textAlign: 'center', color: '#94a3b8', fontSize: 12 }}
        >
          Protected by enterprise-grade security · All sessions are audited
        </Text>
      </div>
    </div>
  );
}

// ── Inline styles (no extra CSS file needed) ─────────────────────────────────
const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight:       '100vh',
    display:         'flex',
    alignItems:      'center',
    justifyContent:  'center',
    background:      'linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0f172a 100%)',
    padding:         '24px 16px',
    position:        'relative',
    overflow:        'hidden',
  },
  blobTop: {
    position:     'absolute',
    top:          '-120px',
    right:        '-100px',
    width:        '400px',
    height:       '400px',
    borderRadius: '50%',
    background:   'radial-gradient(circle, rgba(37,99,235,0.25) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  blobBottom: {
    position:     'absolute',
    bottom:       '-150px',
    left:         '-120px',
    width:        '500px',
    height:       '500px',
    borderRadius: '50%',
    background:   'radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  card: {
    position:        'relative',
    zIndex:          1,
    background:      '#ffffff',
    borderRadius:    '16px',
    padding:         '40px 44px',
    width:           '100%',
    maxWidth:        '440px',
    boxShadow:       '0 25px 60px rgba(0,0,0,0.35)',
  },
  logoRow: {
    display:        'flex',
    alignItems:     'center',
    gap:            '14px',
  },
  logoIcon: {
    width:          '50px',
    height:         '50px',
    borderRadius:   '12px',
    background:     'linear-gradient(135deg, #1d4ed8, #2563eb)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    boxShadow:      '0 4px 14px rgba(37,99,235,0.4)',
    flexShrink:     0,
  },
  label: {
    color:      '#374151',
    fontWeight: 600,
    fontSize:   14,
  },
  input: {
    borderRadius: '8px',
    border:       '1.5px solid #e2e8f0',
  },
  submitBtn: {
    height:        '48px',
    borderRadius:  '10px',
    background:    'linear-gradient(135deg, #1d4ed8, #2563eb)',
    border:        'none',
    fontSize:      '16px',
    fontWeight:    600,
    letterSpacing: '0.02em',
    boxShadow:     '0 4px 14px rgba(37,99,235,0.4)',
  },
};

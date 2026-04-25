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
    <div className="ci-auth-page">
      {/* ── Card ── */}
      <div className="ci-auth-card">
        {/* Heading */}
        <Title level={4} style={{ margin: '0 0 4px', color: '#222', textAlign: 'center' }}>
          ClaimInsight<span style={{ color: '#2563eb' }}>360</span>
        </Title>
        <Text style={{ color: '#888', display: 'block', marginBottom: 20, textAlign: 'center', fontSize: 12 }}>
          Sign in to your account
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

        <Divider style={{ margin: '16px 0' }} />

        <div style={{ textAlign: 'center' }}>
          <Text style={{ color: '#888', fontSize: 12 }}>
            Don&apos;t have an account?{' '}
            <Link to="/register" style={{ color: '#2563eb' }}>
              Create account
            </Link>
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

import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useDispatch }          from 'react-redux';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import { Button, Card, Form, Input, Alert, Checkbox } from 'antd';

import { authApi }         from '../../api/authApi';
import { setCredentials }  from '../../store/slices/authSlice';
import './auth.css';

// ── Validation schema ────────────────────────────────────────────────────────
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

/**
 * Login — minimal centered card. Styling lives in auth.css; this file is
 * just the form logic.
 */
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
    defaultValues: { username: '', password: '', remember: true },
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
      // Restore the user's intended destination if they were bounced here
      // by the axios 401 handler. Falls back to /dashboard for fresh logins.
      let redirectTo = '/dashboard';
      try {
        const stored = sessionStorage.getItem('ci360.redirectAfterLogin');
        if (stored && stored.startsWith('/') && !stored.startsWith('/login')) {
          redirectTo = stored;
        }
        sessionStorage.removeItem('ci360.redirectAfterLogin');
      } catch { /* ignore — sessionStorage may be blocked */ }
      navigate(redirectTo, { replace: true });
    } catch (err: unknown) {
      // axiosInstance interceptor attaches a normalized `userMessage` derived
      // from whichever error envelope the backend service emits.
      const msg =
        (err as { userMessage?: string }).userMessage
        ?? 'Invalid username or password. Please try again.';
      setApiError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <div className="auth-brand">
          <h1 className="auth-brand-logo">ClaimInsight360</h1>
        </div>

        <h2 className="auth-title">Welcome back</h2>
        <p className="auth-subtitle">Sign in to your workspace to continue</p>

        {apiError && (
          <Alert
            type="error"
            message={apiError}
            showIcon
            closable
            onClose={() => setApiError(null)}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form layout="vertical" onFinish={handleSubmit(onSubmit)}>
          <Form.Item
            label="Username or email"
            validateStatus={errors.username ? 'error' : ''}
            help={errors.username?.message}
          >
            <Controller
              name="username"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  placeholder="priya.menon or priya@acme-carrier.com"
                  autoComplete="username"
                  autoFocus
                />
              )}
            />
          </Form.Item>

          <Form.Item
            label="Password"
            validateStatus={errors.password ? 'error' : ''}
            help={errors.password?.message}
          >
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
              )}
            />
          </Form.Item>

          <Form.Item>
            <Controller
              name="remember"
              control={control}
              render={({ field }) => (
                <Checkbox
                  checked={field.value}
                  onChange={(e) => field.onChange(e.target.checked)}
                >
                  Keep me signed in
                </Checkbox>
              )}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} block>
              {loading ? 'Signing in…' : 'Sign in'}
            </Button>
          </Form.Item>
        </Form>

        <div className="auth-footer">
          Don&apos;t have an account?{' '}
          <Link to="/register">Request access</Link>
        </div>
      </Card>
    </div>
  );
}

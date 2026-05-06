import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useDispatch }          from 'react-redux';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import { Button, Form, Input, Alert, Checkbox } from 'antd';
import { Shield, BarChart3, AlertTriangle, Bell, ArrowLeft } from 'lucide-react';

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
      let redirectTo = '/dashboard';
      try {
        const stored = sessionStorage.getItem('ci360.redirectAfterLogin');
        if (stored && stored.startsWith('/') && !stored.startsWith('/login')) {
          redirectTo = stored;
        }
        sessionStorage.removeItem('ci360.redirectAfterLogin');
      } catch { /* ignore */ }
      navigate(redirectTo, { replace: true });
    } catch (err: unknown) {
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
      {/* ── Brand panel (hidden on mobile) ─────────────────────────────── */}
      <aside className="auth-brand-panel">
        <div className="auth-brand-row">
          <div className="auth-brand-mark"><Shield size={16} strokeWidth={2.2} color="#ffffff" /></div>
          <span className="auth-brand-text">ClaimInsight360</span>
        </div>

        <div className="auth-brand-message">
          <span className="auth-brand-tagline">Claims Intelligence Platform</span>
          <h1 className="auth-brand-headline">
            One workspace for every claim signal.
          </h1>
          <p className="auth-brand-sub">
            Real-time KPIs, fraud detection, and adjuster performance —
            all in one role-scoped dashboard.
          </p>
          <ul className="auth-brand-bullets">
            <li><BarChart3 size={15} strokeWidth={2} /> Live TAT, severity & loss-ratio metrics</li>
            <li><AlertTriangle size={15} strokeWidth={2} /> Risk scoring with one-click SIU escalation</li>
            <li><Bell size={15} strokeWidth={2} /> Real-time notifications via Server-Sent Events</li>
          </ul>
        </div>

        <div className="auth-brand-footer">© 2026 ClaimInsight360 · Built for insurance carriers</div>
      </aside>

      {/* ── Form panel ─────────────────────────────────────────────────── */}
      <main className="auth-form-panel">
        <div className="auth-card">
          <Link to="/" className="auth-back-link"><ArrowLeft size={12} /> Back to home</Link>

          <div className="auth-card-brand">
            <div className="auth-brand-mark"><Shield size={14} strokeWidth={2.2} color="#ffffff" /></div>
            <span className="auth-brand-text">ClaimInsight360</span>
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
                    size="large"
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
                    size="large"
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
              <Button type="primary" htmlType="submit" loading={loading} block size="large">
                {loading ? 'Signing in…' : 'Sign in'}
              </Button>
            </Form.Item>
          </Form>

          <div className="auth-footer">
            Don&apos;t have an account?{' '}
            <Link to="/register">Request access</Link>
          </div>
        </div>
      </main>
    </div>
  );
}

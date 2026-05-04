import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useDispatch }          from 'react-redux';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import { Button, Form, Input, Alert, Checkbox } from 'antd';
import { User as UserIcon, Lock, Eye, EyeOff } from 'lucide-react';

import { authApi }         from '../../api/authApi';
import { setCredentials }  from '../../store/slices/authSlice';

// ── Validation schema ────────────────────────────────────────────────────────
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

/**
 * Login — reference screen #01.
 * Split 1:1 layout. Left: blue gradient brand panel with status pill + stats.
 * Right: SSO row above email form, focused input with blue ring,
 * show/hide password toggle inside the input.
 */
export default function LoginPage() {
  const navigate  = useNavigate();
  const dispatch  = useDispatch();
  const [loading, setLoading]   = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [showPw, setShowPw]     = useState(false);

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
      navigate('/dashboard', { replace: true });
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
    <div style={styles.page}>
      <div style={styles.frame}>
        {/* ── Brand panel (left) ── */}
        <div style={styles.brand}>
          <div style={styles.bgCircle1} />
          <div style={styles.bgCircle2} />

          <div style={styles.logo}>
            <div style={styles.logoMark}><div style={styles.logoInner} /></div>
            <div style={styles.logoText}>ClaimInsight360</div>
          </div>

          <div style={{ position: 'relative', zIndex: 1 }}>
            <div style={styles.pill}>
              <span style={styles.pillDot} />
              All systems operational
            </div>
            <h2 style={styles.heroTitle}>
              Claims intelligence,<br />engineered for scale.
            </h2>
            <p style={styles.heroSub}>
              Monitor portfolio KPIs, detect fraud patterns, and act on claim insights in real-time.
            </p>
          </div>

          <div style={styles.stats}>
            <div><div style={styles.statVal}>14.2K</div><div style={styles.statLbl}>CLAIMS</div></div>
            <div><div style={styles.statVal}>99.9%</div><div style={styles.statLbl}>UPTIME</div></div>
            <div><div style={styles.statVal}>42</div><div style={styles.statLbl}>CARRIERS</div></div>
          </div>
        </div>

        {/* ── Form panel (right) ── */}
        <div style={styles.authForm}>
          <h2 style={styles.formTitle}>Welcome back</h2>
          <p style={styles.formSub}>Sign in to your workspace to continue</p>

          {/* SSO row — above email form per spec */}
          <div style={styles.ssoRow}>
            <button type="button" style={styles.sso} disabled>Google</button>
            <button type="button" style={styles.sso} disabled>Microsoft</button>
            <button type="button" style={styles.sso} disabled>SSO</button>
          </div>

          <div style={styles.divider}>
            <div style={styles.dividerLine} />
            <span>or sign in with email</span>
            <div style={styles.dividerLine} />
          </div>

          {apiError && (
            <Alert
              type="error"
              message={apiError}
              showIcon
              closable
              onClose={() => setApiError(null)}
              style={{ marginBottom: 14, borderRadius: 7 }}
            />
          )}

          <Form layout="vertical" onFinish={handleSubmit(onSubmit)}>
            <Form.Item
              label={<span style={styles.labelText}>Username or email</span>}
              validateStatus={errors.username ? 'error' : ''}
              help={errors.username?.message}
              style={{ marginBottom: 14 }}
            >
              <Controller
                name="username"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    prefix={<UserIcon size={14} strokeWidth={1.7} color="#888780" />}
                    placeholder="priya.menon or priya@acme-carrier.com"
                    autoComplete="username"
                    autoFocus
                    style={styles.input}
                  />
                )}
              />
            </Form.Item>

            <Form.Item
              label={
                <span style={{ ...styles.labelText, display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                  <span>Password</span>
                  <Link to="#" style={styles.link}>Forgot?</Link>
                </span>
              }
              validateStatus={errors.password ? 'error' : ''}
              help={errors.password?.message}
              style={{ marginBottom: 8 }}
            >
              <Controller
                name="password"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    type={showPw ? 'text' : 'password'}
                    prefix={<Lock size={14} strokeWidth={1.7} color="#888780" />}
                    suffix={
                      <button
                        type="button"
                        onClick={() => setShowPw(s => !s)}
                        style={styles.showBtn}
                        aria-label={showPw ? 'Hide password' : 'Show password'}
                      >
                        {showPw
                          ? <EyeOff size={13} strokeWidth={1.7} />
                          : <Eye size={13} strokeWidth={1.7} />}
                      </button>
                    }
                    placeholder="Enter your password"
                    autoComplete="current-password"
                    style={styles.input}
                  />
                )}
              />
            </Form.Item>

            <div style={styles.between}>
              <Controller
                name="remember"
                control={control}
                render={({ field }) => (
                  <Checkbox
                    checked={field.value}
                    onChange={(e) => field.onChange(e.target.checked)}
                  >
                    <span style={{ color: '#5F5E5A', fontSize: 11 }}>Keep me signed in</span>
                  </Checkbox>
                )}
              />
              <span style={{ color: '#888780', fontSize: 11 }}>Session · 30 days</span>
            </div>

            <Form.Item style={{ marginBottom: 14 }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                style={styles.submitBtn}
              >
                {loading ? 'Signing in…' : 'Sign in to workspace →'}
              </Button>
            </Form.Item>
          </Form>

          <div style={styles.footerText}>
            Don&apos;t have an account?{' '}
            <Link to="/register" style={styles.link}>Request access</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Styles ───────────────────────────────────────────────────────────────────
const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#F1EFE8',
    padding: '24px 16px',
  },
  frame: {
    width: '100%',
    maxWidth: 960,
    background: '#fff',
    border: '1px solid #E8E6DF',
    borderRadius: 12,
    overflow: 'hidden',
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    minHeight: 560,
  },
  brand: {
    padding: '36px 32px',
    color: '#fff',
    position: 'relative',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    background: 'linear-gradient(135deg, #0C447C 0%, #185FA5 100%)',
  },
  bgCircle1: {
    position: 'absolute', top: -50, right: -50,
    width: 240, height: 240, borderRadius: '50%',
    background: 'rgba(255,255,255,0.04)',
  },
  bgCircle2: {
    position: 'absolute', bottom: -80, left: -60,
    width: 200, height: 200, borderRadius: '50%',
    background: 'rgba(255,255,255,0.03)',
  },
  logo: {
    display: 'flex', alignItems: 'center', gap: 10,
    position: 'relative', zIndex: 1,
  },
  logoMark: {
    width: 32, height: 32, borderRadius: 8,
    background: 'rgba(255,255,255,0.18)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  logoInner: { width: 12, height: 12, background: '#fff', borderRadius: 3 },
  logoText: { fontSize: 15, fontWeight: 500 },
  pill: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    padding: '4px 10px',
    background: 'rgba(255,255,255,0.12)',
    border: '1px solid rgba(255,255,255,0.18)',
    borderRadius: 20, fontSize: 11,
    color: 'rgba(255,255,255,0.9)', marginBottom: 16,
  },
  pillDot: { width: 6, height: 6, borderRadius: '50%', background: '#5DCAA5' },
  heroTitle: {
    fontSize: 22, fontWeight: 500, lineHeight: 1.3,
    marginBottom: 12, color: '#fff',
  },
  heroSub: { fontSize: 13, lineHeight: 1.6, color: 'rgba(255,255,255,0.75)' },
  stats: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12,
    paddingTop: 20, borderTop: '1px solid rgba(255,255,255,0.15)',
    position: 'relative', zIndex: 1,
  },
  statVal: { fontSize: 18, fontWeight: 500, color: '#fff' },
  statLbl: { fontSize: 10, color: 'rgba(255,255,255,0.6)', marginTop: 4, letterSpacing: '0.3px' },
  authForm: {
    padding: '40px 36px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    background: '#fff',
  },
  formTitle: { fontSize: 22, fontWeight: 500, marginBottom: 6, color: '#2C2C2A' },
  formSub: { fontSize: 13, color: '#888780', marginBottom: 24 },
  ssoRow: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8,
    marginBottom: 20,
  },
  sso: {
    height: 38, border: '1px solid #D3D1C7', borderRadius: 7,
    background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
    gap: 6, fontSize: 12, cursor: 'pointer', color: '#2C2C2A',
  },
  divider: {
    display: 'flex', alignItems: 'center', gap: 10,
    margin: '6px 0 14px', fontSize: 11, color: '#888780',
  },
  dividerLine: { flex: 1, height: 1, background: '#D3D1C7' },
  labelText: { fontSize: 11, fontWeight: 500, color: '#5F5E5A' },
  input: {
    height: 38, borderRadius: 7, border: '1px solid #D3D1C7',
    fontSize: 13,
  },
  showBtn: {
    border: 'none', background: 'transparent', cursor: 'pointer',
    color: '#888780', fontSize: 11, display: 'inline-flex', alignItems: 'center',
    padding: 0,
  },
  between: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    margin: '4px 0 18px', fontSize: 11,
  },
  submitBtn: {
    height: 42, borderRadius: 7,
    background: '#185FA5', border: 'none',
    fontSize: 13, fontWeight: 500,
  },
  link: { color: '#185FA5', fontWeight: 500, fontSize: 11 },
  footerText: { fontSize: 12, color: '#888780', textAlign: 'center', marginTop: 4 },
};

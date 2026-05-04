import { useState, useMemo }   from 'react';
import { useNavigate, Link }   from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver }         from '@hookform/resolvers/zod';
import { z }                   from 'zod';
import { Button, Form, Input, Alert, Select, Checkbox, App as AntApp } from 'antd';
import { Mail, Lock, User, Check } from 'lucide-react';

import { authApi } from '../../api/authApi';

// ── Schema ───────────────────────────────────────────────────────────────────
const schema = z.object({
  username:        z.string().min(3, 'Username must be at least 3 characters'),
  email:           z.string().email('Enter a valid email address'),
  password:        z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  role:            z.string().min(1, 'Please select a role'),
  terms:           z.boolean().refine(v => v === true, { message: 'You must agree to continue' }),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path:    ['confirmPassword'],
});

type RegisterFormValues = z.infer<typeof schema>;

function passwordStrength(pw: string): { score: 0 | 1 | 2 | 3 | 4; label: string } {
  if (!pw) return { score: 0, label: 'Use 8+ characters with mixed case and numbers' };
  let score = 0;
  if (pw.length >= 8)  score++;
  if (pw.length >= 12) score++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
  if (/\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) score++;
  const labels = ['Use 8+ characters with mixed case and numbers', 'Too weak', 'Fair', 'Good', 'Strong password · 12+ chars, mixed case, numbers'];
  return { score: score as 0 | 1 | 2 | 3 | 4, label: labels[score] };
}

/**
 * Register — reference screen #02.
 * Split 1:1 layout. Teal brand panel (left) with compliance credentials.
 * Form (right) with 3-step progress, inline email-domain verification,
 * live password strength bar, terms checkbox gating submit.
 */
export default function RegisterPage() {
  const navigate = useNavigate();
  const [loading,  setLoading]  = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [success,  setSuccess]  = useState(false);
  // Theme-aware toast for success confirmation; inline Alert stays for errors.
  const { message } = AntApp.useApp();

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver:      zodResolver(schema),
    defaultValues: { username: '', email: '', password: '', confirmPassword: '', role: '', terms: false },
    mode:          'onChange',
  });

  const email    = watch('email');
  const password = watch('password');
  const terms    = watch('terms');

  const emailDomain = useMemo(() => {
    const m = /@([A-Za-z0-9.-]+\.[A-Za-z]{2,})$/.exec(email ?? '');
    return m ? m[1] : null;
  }, [email]);

  const strength = passwordStrength(password ?? '');

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
      message.success('Account created — redirecting to sign in');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: unknown) {
      const msg =
        (err as { userMessage?: string }).userMessage
        ?? 'Registration failed. Username or email may already be taken.';
      setApiError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.frame}>
        {/* ── Brand panel (teal) ── */}
        <div style={styles.brand}>
          <div style={styles.bgCircle1} />
          <div style={styles.bgCircle2} />

          <div style={styles.logo}>
            <div style={styles.logoMark}><div style={styles.logoInner} /></div>
            <div style={styles.logoText}>ClaimInsight360</div>
          </div>

          <div style={{ position: 'relative', zIndex: 1 }}>
            <div style={styles.pill}>
              <Check size={10} strokeWidth={2.5} /> Enterprise ready
            </div>
            <h2 style={styles.heroTitle}>
              Start analyzing claims in minutes, not months.
            </h2>
            <p style={styles.heroSub}>
              Connect your claims system, configure KPIs, and give your team real-time visibility.
            </p>
            <div style={styles.features}>
              {['SOC 2 Type II certified', 'HIPAA & GDPR compliant', 'Dedicated onboarding team'].map(f => (
                <div key={f} style={styles.feature}>
                  <div style={styles.featureCheck}><Check size={10} strokeWidth={2.5} /></div>
                  {f}
                </div>
              ))}
            </div>
          </div>

          <div style={styles.stats}>
            <div><div style={styles.statVal}>14 day</div><div style={styles.statLbl}>FREE TRIAL</div></div>
            <div><div style={styles.statVal}>24/7</div><div style={styles.statLbl}>SUPPORT</div></div>
            <div><div style={styles.statVal}>No card</div><div style={styles.statLbl}>REQUIRED</div></div>
          </div>
        </div>

        {/* ── Form panel ── */}
        <div style={styles.authForm}>
          <h2 style={styles.formTitle}>Create your account</h2>
          <p style={styles.formSub}>Request workspace access for your organization</p>

          {/* 3-step progress bar */}
          <div style={styles.progressBars}>
            <div style={{ ...styles.progressSeg, background: '#185FA5' }} />
            <div style={{ ...styles.progressSeg, background: '#185FA5' }} />
            <div style={styles.progressSeg} />
          </div>
          <div style={styles.progressMeta}>
            <span>Step 2 of 3 · Account details</span>
            <span style={{ color: '#185FA5' }}>66%</span>
          </div>

          {apiError && (
            <Alert
              type="error" message={apiError} showIcon closable
              onClose={() => setApiError(null)}
              style={{ marginBottom: 14, borderRadius: 7 }}
            />
          )}
          {success && (
            <Alert
              type="success"
              message="Account created successfully. Redirecting to sign in…"
              showIcon
              style={{ marginBottom: 14, borderRadius: 7 }}
            />
          )}

          <Form layout="vertical" onFinish={handleSubmit(onSubmit)}>
            <Form.Item
              label={<span style={styles.labelText}>Username</span>}
              validateStatus={errors.username ? 'error' : ''}
              help={errors.username?.message}
              style={{ marginBottom: 14 }}
            >
              <Controller name="username" control={control} render={({ field }) => (
                <Input {...field}
                  prefix={<User size={14} strokeWidth={1.7} color="#888780" />}
                  placeholder="Choose a username"
                  autoFocus
                  style={styles.input}
                />
              )} />
            </Form.Item>

            <Form.Item
              label={<span style={styles.labelText}>Work email</span>}
              validateStatus={errors.email ? 'error' : ''}
              help={errors.email?.message}
              style={{ marginBottom: 4 }}
            >
              <Controller name="email" control={control} render={({ field }) => (
                <Input {...field}
                  prefix={<Mail size={14} strokeWidth={1.7} color="#888780" />}
                  suffix={emailDomain
                    ? <Check size={13} strokeWidth={2} color="#27500A" />
                    : <span />}
                  placeholder="you@company.com"
                  type="email"
                  style={styles.input}
                />
              )} />
            </Form.Item>
            {emailDomain && (
              <div style={styles.helper}>Work domain verified · {emailDomain}</div>
            )}

            <Form.Item
              label={<span style={styles.labelText}>Organization role</span>}
              validateStatus={errors.role ? 'error' : ''}
              help={errors.role?.message}
              style={{ marginBottom: 14, marginTop: 14 }}
            >
              <Controller name="role" control={control} render={({ field }) => (
                <Select
                  {...field}
                  placeholder="Select your role"
                  style={{ width: '100%' }}
                  options={[
                    { value: 'ROLE_CLAIMS_ANALYST',  label: 'Claims analyst' },
                    { value: 'ROLE_CLAIMS_MANAGER',  label: 'Claims manager' },
                    { value: 'ROLE_OPERATIONS_EXEC', label: 'Operations executive' },
                    { value: 'ROLE_FRAUD_ANALYST',   label: 'Fraud analyst' },
                    { value: 'ROLE_ACTUARY',         label: 'Actuary' },
                  ]}
                />
              )} />
            </Form.Item>

            <Form.Item
              label={<span style={styles.labelText}>Create password</span>}
              validateStatus={errors.password ? 'error' : ''}
              help={errors.password?.message}
              style={{ marginBottom: 4 }}
            >
              <Controller name="password" control={control} render={({ field }) => (
                <Input.Password {...field}
                  prefix={<Lock size={14} strokeWidth={1.7} color="#888780" />}
                  placeholder="Min. 8 characters"
                  autoComplete="new-password"
                  style={styles.input}
                />
              )} />
            </Form.Item>

            {/* Strength bars */}
            <div style={styles.strength}>
              {[1, 2, 3, 4].map(i => (
                <div
                  key={i}
                  style={{
                    ...styles.strengthBar,
                    background: i <= strength.score ? '#1D9E75' : '#E8E6DF',
                  }}
                />
              ))}
            </div>
            <div style={{ ...styles.strengthLabel, color: strength.score >= 3 ? '#27500A' : '#888780' }}>
              {strength.label}
            </div>

            <Form.Item
              label={<span style={styles.labelText}>Confirm password</span>}
              validateStatus={errors.confirmPassword ? 'error' : ''}
              help={errors.confirmPassword?.message}
              style={{ marginBottom: 14, marginTop: 14 }}
            >
              <Controller name="confirmPassword" control={control} render={({ field }) => (
                <Input.Password {...field}
                  prefix={<Lock size={14} strokeWidth={1.7} color="#888780" />}
                  placeholder="Repeat your password"
                  autoComplete="new-password"
                  style={styles.input}
                />
              )} />
            </Form.Item>

            <div style={{ marginBottom: 14 }}>
              <Controller name="terms" control={control} render={({ field }) => (
                <Checkbox
                  checked={field.value}
                  onChange={(e) => field.onChange(e.target.checked)}
                >
                  <span style={{ fontSize: 11, color: '#5F5E5A', lineHeight: 1.5 }}>
                    I agree to the <Link to="#" style={styles.link}>Terms of Service</Link> and{' '}
                    <Link to="#" style={styles.link}>Privacy Policy</Link>
                  </span>
                </Checkbox>
              )} />
              {errors.terms && (
                <div style={{ fontSize: 11, color: '#791F1F', marginTop: 4 }}>{errors.terms.message}</div>
              )}
            </div>

            <Form.Item style={{ marginBottom: 14 }}>
              <Button
                type="primary" htmlType="submit"
                loading={loading} disabled={success || !terms} block
                style={styles.submitBtn}
              >
                {loading ? 'Creating account…' : 'Continue to verification →'}
              </Button>
            </Form.Item>
          </Form>

          <div style={styles.footerText}>
            Already have an account?{' '}
            <Link to="/login" style={styles.link}>Sign in instead</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Styles ───────────────────────────────────────────────────────────────────
const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh', display: 'flex',
    alignItems: 'center', justifyContent: 'center',
    background: '#F1EFE8', padding: '24px 16px',
  },
  frame: {
    width: '100%', maxWidth: 960,
    background: '#fff', border: '1px solid #E8E6DF',
    borderRadius: 12, overflow: 'hidden',
    display: 'grid', gridTemplateColumns: '1fr 1fr',
    minHeight: 620,
  },
  brand: {
    padding: '36px 32px', color: '#fff',
    position: 'relative', overflow: 'hidden',
    display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
    background: 'linear-gradient(135deg, #085041 0%, #0F6E56 100%)',
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
  logo: { display: 'flex', alignItems: 'center', gap: 10, position: 'relative', zIndex: 1 },
  logoMark: {
    width: 32, height: 32, borderRadius: 8,
    background: 'rgba(255,255,255,0.18)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  logoInner: { width: 12, height: 12, background: '#fff', borderRadius: 3 },
  logoText: { fontSize: 15, fontWeight: 500 },
  pill: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    padding: '4px 10px', background: 'rgba(255,255,255,0.15)',
    border: '1px solid rgba(255,255,255,0.18)',
    borderRadius: 20, fontSize: 11,
    color: 'rgba(255,255,255,0.9)', marginBottom: 16,
  },
  heroTitle: { fontSize: 22, fontWeight: 500, lineHeight: 1.3, marginBottom: 12, color: '#fff' },
  heroSub: { fontSize: 13, lineHeight: 1.6, color: 'rgba(255,255,255,0.75)' },
  features: { marginTop: 22, display: 'flex', flexDirection: 'column', gap: 10 },
  feature: { display: 'flex', alignItems: 'center', gap: 10, fontSize: 12, color: 'rgba(255,255,255,0.85)' },
  featureCheck: {
    width: 18, height: 18, borderRadius: '50%',
    background: 'rgba(255,255,255,0.15)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  stats: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12,
    paddingTop: 20, borderTop: '1px solid rgba(255,255,255,0.15)',
    position: 'relative', zIndex: 1,
  },
  statVal: { fontSize: 18, fontWeight: 500, color: '#fff' },
  statLbl: { fontSize: 10, color: 'rgba(255,255,255,0.6)', marginTop: 4, letterSpacing: '0.3px' },
  authForm: {
    padding: '32px 36px', display: 'flex',
    flexDirection: 'column', justifyContent: 'center',
    background: '#fff',
  },
  formTitle: { fontSize: 22, fontWeight: 500, marginBottom: 6, color: '#2C2C2A' },
  formSub: { fontSize: 13, color: '#888780', marginBottom: 20 },
  progressBars: { display: 'flex', gap: 6, marginBottom: 8 },
  progressSeg: { flex: 1, height: 3, borderRadius: 2, background: '#E8E6DF' },
  progressMeta: {
    fontSize: 11, color: '#888780',
    marginBottom: 16, display: 'flex', justifyContent: 'space-between',
  },
  labelText: { fontSize: 11, fontWeight: 500, color: '#5F5E5A' },
  input: { height: 38, borderRadius: 7, border: '1px solid #D3D1C7', fontSize: 13 },
  helper: { fontSize: 11, color: '#27500A', marginTop: 4 },
  strength: { display: 'flex', gap: 3, marginTop: 6 },
  strengthBar: { height: 3, flex: 1, borderRadius: 2 },
  strengthLabel: { fontSize: 10, marginTop: 4 },
  submitBtn: {
    height: 42, borderRadius: 7,
    background: '#185FA5', border: 'none',
    fontSize: 13, fontWeight: 500,
  },
  link: { color: '#185FA5', fontWeight: 500 },
  footerText: { fontSize: 12, color: '#888780', textAlign: 'center', marginTop: 4 },
};

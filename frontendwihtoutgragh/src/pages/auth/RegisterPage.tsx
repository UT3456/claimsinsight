import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import { Button, Form, Input, Alert, Select, Checkbox, App as AntApp } from 'antd';
import { Shield, Users, Lock, CheckCircle2, ArrowLeft, X } from 'lucide-react';

import { authApi } from '../../api/authApi';
import './auth.css';

// ── Schema — matches backend @Pattern complexity rule ───────────────────────
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+\-=]).{8,}$/;

const schema = z.object({
  username:        z.string().min(3, 'Username must be at least 3 characters'),
  email:           z.string().email('Enter a valid email address'),
  password:        z.string()
                    .min(8, 'Password must be at least 8 characters')
                    .regex(PASSWORD_REGEX, 'Password must include upper, lower, digit & special character'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  role:            z.string().min(1, 'Please select a role'),
  terms:           z.boolean().refine(v => v === true, { message: 'You must agree to continue' }),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path:    ['confirmPassword'],
});

type RegisterFormValues = z.infer<typeof schema>;

function passwordChecks(pw: string) {
  return {
    length:  pw.length >= 8,
    upper:   /[A-Z]/.test(pw),
    lower:   /[a-z]/.test(pw),
    digit:   /\d/.test(pw),
    special: /[@$!%*?&#^()_+\-=]/.test(pw),
  };
}

function passwordStrength(checks: ReturnType<typeof passwordChecks>) {
  const score = Object.values(checks).filter(Boolean).length; // 0..5
  const labels = ['Enter a password', 'Very weak', 'Weak', 'Fair', 'Good', 'Strong password'];
  return { score, label: labels[score] };
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const [loading,  setLoading]  = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [success,  setSuccess]  = useState(false);
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

  const password = watch('password') ?? '';
  const terms    = watch('terms');
  const checks   = passwordChecks(password);
  const strength = passwordStrength(checks);

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
    <div className="auth-page">
      {/* ── Brand panel (hidden on mobile) ─────────────────────────────── */}
      <aside className="auth-brand-panel">
        <div className="auth-brand-row">
          <div className="auth-brand-mark"><Shield size={16} strokeWidth={2.2} color="#ffffff" /></div>
          <span className="auth-brand-text">ClaimInsight360</span>
        </div>

        <div className="auth-brand-message">
          <span className="auth-brand-tagline">Get started</span>
          <h1 className="auth-brand-headline">
            Bring clarity to your claims portfolio.
          </h1>
          <p className="auth-brand-sub">
            Request workspace access for your role and start surfacing
            signals your team can act on — same day.
          </p>
          <ul className="auth-brand-bullets">
            <li><Users size={15} strokeWidth={2} /> Role-scoped dashboards out of the box</li>
            <li><Lock size={15} strokeWidth={2} /> JWT auth, refresh tokens, full audit trail</li>
            <li><CheckCircle2 size={15} strokeWidth={2} /> No installs — sign in from any browser</li>
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

          <h2 className="auth-title">Create your account</h2>
          <p className="auth-subtitle">Request workspace access for your organization</p>

          {apiError && (
            <Alert
              type="error" message={apiError} showIcon closable
              onClose={() => setApiError(null)}
              style={{ marginBottom: 16 }}
            />
          )}
          {success && (
            <Alert
              type="success"
              message="Account created successfully. Redirecting to sign in…"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}

          <Form layout="vertical" onFinish={handleSubmit(onSubmit)}>
            <Form.Item
              label="Username"
              validateStatus={errors.username ? 'error' : ''}
              help={errors.username?.message}
            >
              <Controller name="username" control={control} render={({ field }) => (
                <Input {...field} placeholder="Choose a username" autoFocus size="large" />
              )} />
            </Form.Item>

            <Form.Item
              label="Work email"
              validateStatus={errors.email ? 'error' : ''}
              help={errors.email?.message}
            >
              <Controller name="email" control={control} render={({ field }) => (
                <Input {...field} placeholder="you@company.com" type="email" size="large" />
              )} />
            </Form.Item>

            <Form.Item
              label="Organization role"
              validateStatus={errors.role ? 'error' : ''}
              help={errors.role?.message}
            >
              <Controller name="role" control={control} render={({ field }) => (
                <Select
                  {...field}
                  placeholder="Select your role"
                  size="large"
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
              label="Password"
              validateStatus={errors.password ? 'error' : ''}
              help={errors.password?.message}
              style={{ marginBottom: 4 }}
            >
              <Controller name="password" control={control} render={({ field }) => (
                <Input.Password {...field} placeholder="Create a strong password" autoComplete="new-password" size="large" />
              )} />
            </Form.Item>

            {/* Strength bar */}
            <div className="auth-strength">
              {[1, 2, 3, 4, 5].map(i => (
                <div
                  key={i}
                  className="auth-strength-bar"
                  style={{ background: i <= strength.score
                    ? (strength.score <= 2 ? '#B22C2B'
                      : strength.score <= 3 ? '#C77800'
                      : '#1F8A4D')
                    : undefined }}
                />
              ))}
            </div>
            <div className="auth-strength-label">{strength.label}</div>

            {/* Live requirement checklist */}
            <div className="auth-pw-reqs">
              <Req met={checks.length}  label="At least 8 characters" />
              <Req met={checks.upper}   label="One uppercase letter" />
              <Req met={checks.lower}   label="One lowercase letter" />
              <Req met={checks.digit}   label="One digit (0-9)" />
              <Req met={checks.special} label="One special character" />
            </div>

            <Form.Item
              label="Confirm password"
              validateStatus={errors.confirmPassword ? 'error' : ''}
              help={errors.confirmPassword?.message}
            >
              <Controller name="confirmPassword" control={control} render={({ field }) => (
                <Input.Password {...field} placeholder="Repeat your password" autoComplete="new-password" size="large" />
              )} />
            </Form.Item>

            <Form.Item>
              <Controller name="terms" control={control} render={({ field }) => (
                <Checkbox
                  checked={field.value}
                  onChange={(e) => field.onChange(e.target.checked)}
                >
                  I agree to the Terms of Service and Privacy Policy
                </Checkbox>
              )} />
              {errors.terms && (
                <div style={{ fontSize: 12, color: 'var(--ci-danger-text)', marginTop: 4 }}>
                  {errors.terms.message}
                </div>
              )}
            </Form.Item>

            <Form.Item style={{ marginBottom: 0 }}>
              <Button
                type="primary" htmlType="submit"
                loading={loading} disabled={success || !terms} block size="large"
              >
                {loading ? 'Creating account…' : 'Create account'}
              </Button>
            </Form.Item>
          </Form>

          <div className="auth-footer">
            Already have an account?{' '}
            <Link to="/login">Sign in instead</Link>
          </div>
        </div>
      </main>
    </div>
  );
}

// Tiny helper for the password-requirements checklist
function Req({ met, label }: { met: boolean; label: string }) {
  return (
    <div className={`auth-pw-req ${met ? 'met' : ''}`}>
      <span className="auth-pw-req-mark">
        {met ? <CheckCircle2 size={12} strokeWidth={2.2} /> : <X size={12} strokeWidth={2} />}
      </span>
      {label}
    </div>
  );
}

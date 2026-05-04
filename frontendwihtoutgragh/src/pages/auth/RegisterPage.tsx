import { useState }             from 'react';
import { useNavigate, Link }    from 'react-router-dom';
import { useForm, Controller }  from 'react-hook-form';
import { zodResolver }          from '@hookform/resolvers/zod';
import { z }                    from 'zod';
import { Button, Card, Form, Input, Alert, Select, Checkbox, App as AntApp } from 'antd';

import { authApi } from '../../api/authApi';
import './auth.css';

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
  const labels = [
    'Use 8+ characters with mixed case and numbers',
    'Too weak',
    'Fair',
    'Good',
    'Strong password',
  ];
  return { score: score as 0 | 1 | 2 | 3 | 4, label: labels[score] };
}

/**
 * Register — minimal centered card. Styling lives in auth.css; this file is
 * just the form logic.
 */
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

  const password = watch('password');
  const terms    = watch('terms');
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
    <div className="auth-page">
      <Card className="auth-card">
        <div className="auth-brand">
          <h1 className="auth-brand-logo">ClaimInsight360</h1>
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
              <Input {...field} placeholder="Choose a username" autoFocus />
            )} />
          </Form.Item>

          <Form.Item
            label="Work email"
            validateStatus={errors.email ? 'error' : ''}
            help={errors.email?.message}
          >
            <Controller name="email" control={control} render={({ field }) => (
              <Input {...field} placeholder="you@company.com" type="email" />
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
              <Input.Password {...field} placeholder="Min. 8 characters" autoComplete="new-password" />
            )} />
          </Form.Item>

          {/* Password-strength bars */}
          <div className="auth-strength">
            {[1, 2, 3, 4].map(i => (
              <div
                key={i}
                className="auth-strength-bar"
                style={{ background: i <= strength.score ? 'var(--ci-success-text)' : undefined }}
              />
            ))}
          </div>
          <div className="auth-strength-label">{strength.label}</div>

          <Form.Item
            label="Confirm password"
            validateStatus={errors.confirmPassword ? 'error' : ''}
            help={errors.confirmPassword?.message}
          >
            <Controller name="confirmPassword" control={control} render={({ field }) => (
              <Input.Password {...field} placeholder="Repeat your password" autoComplete="new-password" />
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
              loading={loading} disabled={success || !terms} block
            >
              {loading ? 'Creating account…' : 'Create account'}
            </Button>
          </Form.Item>
        </Form>

        <div className="auth-footer">
          Already have an account?{' '}
          <Link to="/login">Sign in instead</Link>
        </div>
      </Card>
    </div>
  );
}

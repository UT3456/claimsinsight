import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '@/hooks';
import { setUser } from '@/store/authSlice';
import { authService } from '@/services/authService';
import { Card, CardBody, FormInput, Button, Alert } from '@/components';
import { useForm } from '@/hooks';

const LoginPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const form = useForm({ username: '', password: '' });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const username = form.values.username as string;
      const password = form.values.password as string;

      if (!username || !password) {
        setError('Please enter username and password');
        setLoading(false);
        return;
      }

      console.log('Attempting login with:', { username });
      
      const response = await authService.login({
        username,
        password,
      });

      console.log('Login successful:', response);

      if (response && response.user && response.token) {
        dispatch(setUser({ user: response.user, token: response.token }));
        navigate('/');
      } else {
        setError('Invalid login response');
      }
    } catch (err) {
      console.error('Login error:', err);
      let errorMsg = 'Login failed';
      
      if (err instanceof Error) {
        errorMsg = err.message;
      } else if (typeof err === 'object' && err !== null && 'message' in err) {
        errorMsg = (err as any).message;
      }
      
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f9fafb',
      }}
    >
      <Card style={{ maxWidth: '400px', width: '100%' }}>
        <CardBody>
          <h2 style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
            ClaimInsight360
          </h2>

          {error && <Alert type="danger">{error}</Alert>}

          <form onSubmit={handleSubmit}>
            <FormInput
              type="text"
              name="username"
              placeholder="Username"
              value={form.values.username}
              onChange={form.handleChange}
              required
            />

            <FormInput
              type="password"
              name="password"
              placeholder="Password"
              value={form.values.password}
              onChange={form.handleChange}
              required
            />

            <Button
              type="submit"
              isLoading={loading}
              style={{ width: '100%', marginTop: '1rem' }}
            >
              Sign In
            </Button>
          </form>

          <p
            style={{
              marginTop: '1rem',
              textAlign: 'center',
              fontSize: '0.875rem',
              color: '#6b7280',
            }}
          >
            Demo credentials: admin / password
          </p>
        </CardBody>
      </Card>
    </div>
  );
};

export default LoginPage;

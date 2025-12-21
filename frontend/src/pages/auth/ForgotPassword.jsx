import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import useFormErrors from '../../hooks/useFormErrors';
import PrimaryButton from '../../components/PrimaryButton';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [serverFieldErrors, setServerFieldErrors] = useState(null);
  const { getFieldError } = useFormErrors(serverFieldErrors);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setMessage('');
    setServerFieldErrors(null);
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      setServerFieldErrors({ email: 'Please enter a valid email address' });
      return;
    }
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', { email });
      setMessage('Check your email for a 6-digit reset code. You can now reset your password.');
    } catch (err) {
      const data = err.response?.data;
      if (data?.fieldErrors) setServerFieldErrors(data.fieldErrors);
      else setServerFieldErrors({ general: data?.message || 'Unable to send reset token' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card">
      <h2>Forgot Password</h2>
      <form onSubmit={submit}>
        <input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Email"
          disabled={loading}
          aria-invalid={!!getFieldError('email')}
        />
        {getFieldError('email') && <div className="error">{getFieldError('email')}</div>}

        <PrimaryButton type="submit" className="w-full" disabled={loading}>
          {loading ? 'Sending...' : 'Send Reset Code'}
        </PrimaryButton>
      </form>
      {getFieldError('general') && <div className="error">{getFieldError('general')}</div>}
      {message && (
        <div className="info" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <div>{message}</div>
          <PrimaryButton type="button" variant="secondary" onClick={() => navigate('/reset-password')}>
            Continue to Reset Password
          </PrimaryButton>
        </div>
      )}
    </div>
  );
}

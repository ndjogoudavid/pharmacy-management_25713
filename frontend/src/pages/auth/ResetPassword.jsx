import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../../api';
import useFormErrors from '../../hooks/useFormErrors';
import PrimaryButton from '../../components/PrimaryButton';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [token, setToken] = useState(params.get('token') || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [serverFieldErrors, setServerFieldErrors] = useState(null);
  const { getFieldError } = useFormErrors(serverFieldErrors);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setMessage('');
    setServerFieldErrors(null);
    if (!/^\d{6}$/.test(token)) {
      setServerFieldErrors({ token: 'Reset token must be 6 digits' });
      return;
    }
    if (!newPassword || newPassword.length < 6) {
      setServerFieldErrors({ newPassword: 'Password must be at least 6 characters' });
      return;
    }
    if (newPassword != confirmPassword) {
      setServerFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }
    setLoading(true);
    try {
      await api.post('/auth/reset-password', { token, newPassword });
      setMessage('Password reset successful. You can log in.');
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      const data = err.response?.data;
      if (data?.fieldErrors) {
        setServerFieldErrors(data.fieldErrors);
      } else {
        setServerFieldErrors({ general: data?.message || 'Failed to reset password' });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card">
      <h2>Reset Password</h2>
      <p style={{ marginTop: '-0.5rem', color: '#64748b' }}>
        Enter the 6-digit reset code from your email and choose a new password.
      </p>
      <form onSubmit={submit}>
        <input
          value={token}
          onChange={(e) => setToken(e.target.value.replace(/[^0-9]/g, ''))}
          placeholder="6-digit reset code"
          maxLength={6}
          disabled={loading}
          aria-invalid={!!getFieldError('token')}
        />
        {getFieldError('token') && <div className="error">{getFieldError('token')}</div>}

        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="New password"
          disabled={loading}
          aria-invalid={!!getFieldError('newPassword')}
        />
        {getFieldError('newPassword') && <div className="error">{getFieldError('newPassword')}</div>}

        <input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="Confirm new password"
          disabled={loading}
          aria-invalid={!!getFieldError('confirmPassword')}
        />
        {getFieldError('confirmPassword') && (
          <div className="error">{getFieldError('confirmPassword')}</div>
        )}

        <PrimaryButton type="submit" className="w-full" disabled={loading}>
          {loading ? 'Resetting...' : 'Reset'}
        </PrimaryButton>
      </form>
      {getFieldError('general') && <div className="error">{getFieldError('general')}</div>}
      {message && <div className="info">{message}</div>}
    </div>
  );
}

import { useLocation, useNavigate } from 'react-router-dom';
import { useState, useMemo } from 'react';
import api from '../../api';
import { useAuth } from '../../context/AuthContext';
import useFormErrors from '../../hooks/useFormErrors';
import PrimaryButton from '../../components/PrimaryButton';

export default function Verify2FA() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const emailFromState = location.state?.email || '';
  const [email, setEmail] = useState(emailFromState);
  const [code, setCode] = useState('');
  const [serverFieldErrors, setServerFieldErrors] = useState(null);
  const { getFieldError } = useFormErrors(serverFieldErrors);
  const [submitting, setSubmitting] = useState(false);

  const isCodeValid = useMemo(() => /^\d{6}$/.test(code), [code]);

  const submit = async (e) => {
    e.preventDefault();
    if (!isCodeValid) {
      setServerFieldErrors({ code: 'Code must be exactly 6 digits' });
      return;
    }
    setSubmitting(true);
    setServerFieldErrors(null);
    try {
      const res = await api.post('/auth/verify-2fa', { email, code });
      login(res.data.token, res.data.role, res.data.email, res.data.name);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      // prefer structured field errors when available
      if (data?.fieldErrors) {
        setServerFieldErrors(data.fieldErrors);
      } else {
        setServerFieldErrors({ code: 'Invalid or expired code' });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-card">
      <h2>Verify 2FA</h2>
      <form onSubmit={submit}>
        <input
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Email"
          required
          type="email"
        />
        <input
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ''))}
          placeholder="6-digit code"
          aria-invalid={!!getFieldError('code')}
          maxLength={6}
        />
        {getFieldError('code') && <div className="error">{getFieldError('code')}</div>}
        <PrimaryButton
          type="submit"
          className="w-full"
          disabled={!isCodeValid || submitting}
        >
          {submitting ? 'Verifying…' : 'Verify'}
        </PrimaryButton>
      </form>
      {getFieldError('general') && <div className="error">{getFieldError('general')}</div>}
    </div>
  );
}

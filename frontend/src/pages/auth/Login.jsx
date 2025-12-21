import { useState } from 'react';
import { Eye, EyeOff, Pill } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../api';
import { useAuth } from '../../context/AuthContext';

export default function Login() {
  const navigate = useNavigate();
  const { email: storedEmail } = useAuth();
  const [email, setEmail] = useState(storedEmail || '');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/auth/login', { email, password });
      navigate('/verify-2fa', { state: { email } });
    } catch (err) {
      setError(err?.response?.data?.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Left side - branding */}
      <div className="login-branding">
        <div className="login-branding-content">
          <div className="login-logo">
            <Pill size={48} />
          </div>
          <h1>BDCare</h1>
          <p>Pharmacy Management System</p>
          <div className="login-features">
            <div className="login-feature">✓ Inventory Management</div>
            <div className="login-feature">✓ Prescription Tracking</div>
            <div className="login-feature">✓ Billing & Payments</div>
            <div className="login-feature">✓ Secure 2FA Login</div>
          </div>
        </div>
      </div>

      {/* Right side - form */}
      <div className="login-form-container">
        <form className="login-form" onSubmit={submit}>
          <div className="login-form-header">
            <h2>Welcome Back</h2>
            <p>Sign in to your account</p>
          </div>

          {error && <div className="login-error">{error}</div>}

          <div className="login-field">
            <label>Email Address</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@pharmacy.com"
              required
              autoFocus
            />
          </div>

          <div className="login-field">
            <label>Password</label>
            <div className="login-password-wrapper">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                className="login-password-toggle"
                onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button type="submit" className="login-submit" disabled={loading}>
            {loading ? 'Sending code...' : 'Continue with 2FA'}
          </button>

          <div className="login-footer">
            <a href="/forgot-password">Forgot your password?</a>
          </div>
        </form>
      </div>

      <style>{`
        .login-page {
          min-height: 100vh;
          display: flex;
          background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
        }

        .login-branding {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 3rem;
          background: linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%);
          color: white;
        }

        .login-branding-content {
          max-width: 400px;
        }

        .login-logo {
          width: 80px;
          height: 80px;
          background: rgba(255,255,255,0.2);
          border-radius: 20px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 1.5rem;
          backdrop-filter: blur(10px);
        }

        .login-branding h1 {
          font-size: 2.5rem;
          font-weight: 700;
          margin: 0 0 0.5rem 0;
          letter-spacing: -1px;
        }

        .login-branding > .login-branding-content > p {
          font-size: 1.1rem;
          opacity: 0.9;
          margin: 0 0 2rem 0;
        }

        .login-features {
          display: flex;
          flex-direction: column;
          gap: 0.75rem;
        }

        .login-feature {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-size: 1rem;
          opacity: 0.95;
        }

        .login-form-container {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 3rem;
        }

        .login-form {
          width: 100%;
          max-width: 400px;
          background: white;
          border-radius: 16px;
          padding: 2.5rem;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        }

        .login-form-header {
          text-align: center;
          margin-bottom: 2rem;
        }

        .login-form-header h2 {
          font-size: 1.75rem;
          font-weight: 700;
          color: #0f172a;
          margin: 0 0 0.5rem 0;
        }

        .login-form-header p {
          color: #64748b;
          margin: 0;
        }

        .login-error {
          background: #fef2f2;
          color: #dc2626;
          padding: 0.75rem 1rem;
          border-radius: 8px;
          margin-bottom: 1.5rem;
          font-size: 0.875rem;
          text-align: center;
        }

        .login-field {
          margin-bottom: 1.25rem;
        }

        .login-field label {
          display: block;
          font-size: 0.875rem;
          font-weight: 500;
          color: #374151;
          margin-bottom: 0.5rem;
        }

        .login-field input {
          width: 100%;
          padding: 0.875rem 1rem;
          border: 2px solid #e5e7eb;
          border-radius: 10px;
          font-size: 1rem;
          transition: all 0.2s;
          background: #f9fafb;
        }

        .login-field input:focus {
          outline: none;
          border-color: #10b981;
          background: white;
          box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1);
        }

        .login-password-wrapper {
          position: relative;
        }

        .login-password-wrapper input {
          padding-right: 3rem;
        }

        .login-password-toggle {
          position: absolute;
          right: 1rem;
          top: 50%;
          transform: translateY(-50%);
          background: none;
          border: none;
          color: #6b7280;
          cursor: pointer;
          padding: 0;
          display: flex;
          align-items: center;
        }

        .login-password-toggle:hover {
          color: #374151;
        }

        .login-submit {
          width: 100%;
          padding: 1rem;
          background: linear-gradient(135deg, #10b981 0%, #059669 100%);
          color: white;
          border: none;
          border-radius: 10px;
          font-size: 1rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;
          margin-top: 0.5rem;
        }

        .login-submit:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 10px 20px rgba(16, 185, 129, 0.3);
        }

        .login-submit:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        .login-footer {
          text-align: center;
          margin-top: 1.5rem;
        }

        .login-footer a {
          color: #10b981;
          text-decoration: none;
          font-size: 0.875rem;
          font-weight: 500;
        }

        .login-footer a:hover {
          text-decoration: underline;
        }

        @media (max-width: 900px) {
          .login-page {
            flex-direction: column;
          }
          .login-branding {
            padding: 2rem;
          }
          .login-branding-content {
            text-align: center;
          }
          .login-logo {
            margin: 0 auto 1rem;
          }
          .login-features {
            align-items: center;
          }
        }
      `}</style>
    </div>
  );
}

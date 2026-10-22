import { useState } from 'react';
import { ArrowRight, ClipboardList, Eye, EyeOff, PackageCheck, Pill } from 'lucide-react';
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
    <main className="login-page">
      <section className="login-story" aria-labelledby="brand-title">
        <div className="login-story-inner">
          <a className="login-brand" href="/login" aria-label="BDCare home">
            <span className="login-brand-mark"><Pill size={21} strokeWidth={2.2} /></span>
            <span>BDCare</span>
          </a>

          <div className="login-story-copy">
            <p className="login-product-label">Pharmacy management</p>
            <h1 id="brand-title">Care for the details. Time for the people.</h1>
            <p className="login-story-description">
              Keep medicine stock, prescriptions, and billing moving together in one workspace.
            </p>
          </div>

          <div className="login-workflow" aria-label="What you can manage with BDCare">
            <div className="login-workflow-item">
              <span className="login-workflow-icon"><PackageCheck size={19} /></span>
              <span><strong>Medicine stock</strong><small>Inventory at a glance</small></span>
            </div>
            <div className="login-workflow-item">
              <span className="login-workflow-icon"><ClipboardList size={19} /></span>
              <span><strong>Prescriptions &amp; billing</strong><small>Connected pharmacy workflows</small></span>
            </div>
          </div>
        </div>
        <div className="login-story-art" aria-hidden="true">
          <div className="login-art-orbit login-art-orbit-one" />
          <div className="login-art-orbit login-art-orbit-two" />
          <div className="login-art-capsule"><span /><span /></div>
          <div className="login-art-spark login-art-spark-one" />
          <div className="login-art-spark login-art-spark-two" />
        </div>
        <p className="login-story-foot">A calmer way to run the pharmacy day.</p>
      </section>

      <section className="login-panel" aria-labelledby="login-heading">
        <div className="login-panel-inner">
          <div className="login-mobile-brand" aria-hidden="true">
            <span className="login-brand-mark"><Pill size={20} strokeWidth={2.2} /></span>
            <span>BDCare</span>
          </div>

          <form className="login-form" onSubmit={submit}>
            <div className="login-form-header">
              <h2 id="login-heading">Welcome back</h2>
              <p>Sign in to your pharmacy workspace.</p>
            </div>

            {error && <div className="login-error" role="alert">{error}</div>}

            <div className="login-field">
              <label htmlFor="login-email">Email address</label>
              <input
                id="login-email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="name@pharmacy.com"
                autoComplete="username"
                required
                autoFocus
              />
            </div>

            <div className="login-field">
              <div className="login-label-row">
                <label htmlFor="login-password">Password</label>
                <a href="/forgot-password">Forgot password?</a>
              </div>
              <div className="login-password-wrapper">
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button type="submit" className="login-submit" disabled={loading}>
              <span>{loading ? 'Sending verification code…' : 'Sign in'}</span>
              {!loading && <ArrowRight size={18} aria-hidden="true" />}
            </button>

            <p className="login-security-note">
              <span className="login-security-dot" aria-hidden="true" />
              A verification code will be sent to confirm it’s you.
            </p>
          </form>
          <p className="login-panel-foot">BDCare <span aria-hidden="true">·</span> Pharmacy Management System</p>
        </div>
      </section>

      <style>{`
        .login-page {
          --login-ink: #163c36;
          --login-muted: #667d75;
          min-height: 100vh;
          min-height: 100svh;
          display: grid;
          grid-template-columns: minmax(390px, 0.92fr) minmax(480px, 1.08fr);
          background: #f7faf7;
          color: var(--login-ink);
          font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        }

        .login-story {
          min-height: 100vh;
          min-height: 100svh;
          position: relative;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          overflow: hidden;
          isolation: isolate;
          padding: clamp(2.5rem, 5vw, 5rem) clamp(2.25rem, 6vw, 6.5rem);
          background: #123f36;
          color: #f3f7ef;
        }

        .login-story::before {
          content: '';
          position: absolute;
          z-index: -1;
          inset: 0;
          background: linear-gradient(145deg, rgba(15, 63, 53, .1) 24%, rgba(7, 37, 32, .7) 100%);
        }

        .login-story-inner { position: relative; z-index: 1; max-width: 470px; }
        .login-brand, .login-mobile-brand { display: inline-flex; align-items: center; gap: .72rem; color: inherit; text-decoration: none; font-size: 1.18rem; font-weight: 700; letter-spacing: -.035em; }
        .login-brand-mark { width: 40px; height: 40px; display: inline-grid; place-items: center; border-radius: 13px; background: #e2f0d7; color: #15563f; }
        .login-story-copy { margin-top: clamp(4.5rem, 10vh, 8rem); }
        .login-product-label { margin: 0 0 1rem; color: #b9d4b1; font-size: .76rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
        .login-story h1 { max-width: 12ch; margin: 0; color: #f4f6ed; font-family: 'Iowan Old Style', Baskerville, 'Times New Roman', serif; font-size: clamp(2.8rem, 4vw, 4.15rem); font-weight: 600; line-height: 1.07; letter-spacing: -.045em; text-wrap: balance; }
        .login-story-description { max-width: 36ch; margin: 1.35rem 0 0; color: #c9d8cb; font-size: 1.03rem; line-height: 1.7; }
        .login-workflow { display: grid; gap: 1.25rem; margin-top: clamp(2.5rem, 7vh, 4.25rem); }
        .login-workflow-item { display: flex; align-items: center; gap: .9rem; }
        .login-workflow-icon { width: 42px; height: 42px; flex: 0 0 auto; display: grid; place-items: center; border: 1px solid rgba(203, 224, 196, .22); border-radius: 14px; color: #e0edcf; background: rgba(236, 246, 230, .08); }
        .login-workflow-item strong, .login-workflow-item small { display: block; }
        .login-workflow-item strong { color: #f1f5ed; font-size: .9rem; font-weight: 600; }
        .login-workflow-item small { margin-top: .2rem; color: #bbd0c0; font-size: .8rem; }
        .login-story-foot { position: relative; z-index: 1; margin: 3rem 0 0; color: #a9c5b2; font-size: .78rem; }
        .login-story-art { position: absolute; z-index: -1; width: min(42vw, 560px); aspect-ratio: 1; right: -25%; bottom: 8%; pointer-events: none; }
        .login-art-orbit { position: absolute; inset: 0; border: 1px solid rgba(219, 237, 209, .11); border-radius: 50%; }
        .login-art-orbit-one { transform: scale(.72) rotate(-28deg); }
        .login-art-orbit-two { transform: scale(.48) rotate(25deg); }
        .login-art-capsule { position: absolute; top: 29%; left: 30%; width: 39%; height: 20%; display: flex; overflow: hidden; border: 1px solid rgba(255,255,255,.42); border-radius: 999px; transform: rotate(-38deg); box-shadow: 0 30px 80px rgba(5, 28, 23, .3); }
        .login-art-capsule span { flex: 1; background: #e5efd8; }
        .login-art-capsule span + span { border-left: 1px solid rgba(16, 71, 55, .16); background: #99c9a4; }
        .login-art-spark { position: absolute; width: 8px; height: 8px; border-radius: 50%; background: #d4e8b8; box-shadow: 0 0 24px rgba(215, 239, 181, .6); }
        .login-art-spark-one { top: 20%; left: 65%; }
        .login-art-spark-two { top: 69%; left: 19%; width: 5px; height: 5px; }

        .login-panel { display: grid; place-items: center; padding: 3rem clamp(2rem, 6vw, 6rem); }
        .login-panel-inner { width: min(100%, 410px); }
        .login-mobile-brand { display: none; margin-bottom: 3.5rem; color: var(--login-ink); }
        .login-mobile-brand .login-brand-mark { background: #e3f0dc; }
        .login-form { width: 100%; }
        .login-form-header { margin-bottom: 2.35rem; }
        .login-form-header h2 { margin: 0; color: #173d35; font-family: 'Iowan Old Style', Baskerville, 'Times New Roman', serif; font-size: clamp(2rem, 3vw, 2.45rem); font-weight: 600; letter-spacing: -.04em; line-height: 1.12; }
        .login-form-header p { margin: .7rem 0 0; color: var(--login-muted); font-size: .94rem; }
        .login-error { margin: 0 0 1.25rem; padding: .85rem 1rem; border: 1px solid #f0caca; border-radius: 10px; background: #fff4f2; color: #9c3029; font-size: .875rem; line-height: 1.45; }
        .login-field { margin-bottom: 1.35rem; }
        .login-field label, .login-label-row label { display: block; margin-bottom: .55rem; color: #29483f; font-size: .84rem; font-weight: 600; }
        .login-label-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
        .login-label-row a { margin-bottom: .55rem; color: #24694f; font-size: .79rem; font-weight: 600; text-decoration: none; text-underline-offset: 3px; }
        .login-label-row a:hover { text-decoration: underline; }
        .login-field input { box-sizing: border-box; width: 100%; min-height: 52px; padding: .82rem .95rem; border: 1px solid #cbd8cf; border-radius: 10px; outline: none; background: #fff; color: #193d35; font: inherit; font-size: .92rem; transition: border-color .16s ease, box-shadow .16s ease, background .16s ease; }
        .login-field input::placeholder { color: #82958b; opacity: 1; }
        .login-field input:hover { border-color: #9cb5a6; }
        .login-field input:focus { border-color: #267255; box-shadow: 0 0 0 3px rgba(38, 114, 85, .16); }
        .login-password-wrapper { position: relative; }
        .login-password-wrapper input { padding-right: 3.25rem; }
        .login-password-toggle { position: absolute; top: 50%; right: .72rem; display: grid; width: 38px; height: 38px; place-items: center; border: 0; border-radius: 8px; background: transparent; color: #587368; cursor: pointer; transform: translateY(-50%); }
        .login-password-toggle:hover { background: #eff5ef; color: #194f3d; }
        .login-password-toggle:focus-visible, .login-label-row a:focus-visible, .login-brand:focus-visible { outline: 3px solid #65a987; outline-offset: 3px; }
        .login-submit { display: flex; align-items: center; justify-content: center; gap: .65rem; width: 100%; min-height: 53px; margin-top: .45rem; padding: .85rem 1rem; border: 1px solid #145d46; border-radius: 10px; background: #176b50; color: white; font: inherit; font-size: .91rem; font-weight: 700; cursor: pointer; transition: background .16s ease, box-shadow .16s ease, transform .16s ease; }
        .login-submit:hover:not(:disabled) { background: #105941; box-shadow: 0 8px 18px rgba(19, 88, 64, .16); transform: translateY(-1px); }
        .login-submit:focus-visible { outline: 3px solid #73ad8d; outline-offset: 3px; }
        .login-submit:disabled { opacity: .72; cursor: wait; }
        .login-security-note { display: flex; align-items: flex-start; justify-content: center; gap: .55rem; margin: 1.2rem 0 0; color: #708379; font-size: .77rem; line-height: 1.5; text-align: center; }
        .login-security-dot { width: 7px; height: 7px; flex: 0 0 auto; margin-top: .35rem; border-radius: 50%; background: #5d9c75; }
        .login-panel-foot { margin: 3.5rem 0 0; color: #91a198; font-size: .73rem; text-align: center; }
        .login-panel-foot span { padding: 0 .25rem; color: #b2beb7; }

        @media (max-width: 900px) {
          .login-page { grid-template-columns: minmax(300px, .85fr) minmax(390px, 1.15fr); }
          .login-story { padding: 2.5rem 2rem; }
          .login-story h1 { font-size: clamp(2.45rem, 5vw, 3.3rem); }
          .login-panel { padding: 2.5rem; }
        }
        @media (max-width: 840px) {
          .login-page { min-height: 100svh; display: block; padding: 0; background: #f7faf7; }
          .login-story { display: none; }
          .login-panel { min-height: 100svh; box-sizing: border-box; display: flex; align-items: center; padding: 2rem clamp(1.25rem, 6vw, 2rem); }
          .login-panel-inner { max-width: 420px; margin: 0 auto; }
          .login-mobile-brand { display: inline-flex; margin-bottom: clamp(2.25rem, 8vh, 4rem); }
          .login-form-header { margin-bottom: 2rem; }
          .login-panel-foot { margin-top: 3rem; }
        }
        @media (prefers-reduced-motion: reduce) {
          .login-field input, .login-submit { transition: none; }
        }
      `}</style>
    </main>
  );
}

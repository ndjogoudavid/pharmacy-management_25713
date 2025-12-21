import { useEffect, useState } from 'react';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import LocationPicker from '../components/LocationPicker';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { setUsername } = useAuth();
  const [me, setMe] = useState(null);
  const [locationPath, setLocationPath] = useState(null);
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    locationId: '',
    role: '',
    password: '',
  });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    const load = async () => {
      const res = await api.get('/persons/me');
      setMe(res.data);
      setForm({
        name: res.data.name || '',
        email: res.data.email || '',
        phone: res.data.phone || '',
        locationId: res.data.locationId || '',
        role: res.data.role || '',
        password: '',
      });
      try {
        const pathRes = await api.get(`/persons/${res.data.id}/location-path`);
        setLocationPath(pathRes.data);
      } catch {
        setLocationPath(null);
      }
    };
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();
    if (!me) return;
    setSaving(true);
    setMessage(null);
    try {
      await api.put('/persons/me', {
        name: form.name,
        email: form.email,
        phone: form.phone,
        role: form.role,
        locationId: form.locationId,
        password: form.password || undefined,
      });
      // Update topbar username immediately
      setUsername(form.name);
      setMessage({ type: 'success', text: 'Profile updated successfully.' });
      setForm((prev) => ({ ...prev, password: '' }));
    } catch (e) {
      const data = e.response?.data;
      const msg =
        e.userMessage ||
        data?.message ||
        data?.error ||
        data ||
        'Failed to update profile';
      setMessage({ type: 'error', text: typeof msg === 'string' ? msg : 'Failed to update profile' });
    } finally {
      setSaving(false);
    }
  };

  if (!me) {
    return <div>Loading profile...</div>;
  }

  return (
    <div>
      <div className="section-title">My Profile</div>
      <div className="card">
        <p style={{ fontSize: '0.85rem', color: 'var(--ph-text-muted)', marginBottom: '1rem' }}>
          Keep your contact information up to date. Your role is managed by the system
          administrator.
        </p>
        {message && (
          <div className={message.type === 'error' ? 'error' : 'info'} style={{ marginBottom: '0.75rem' }}>
            {message.text}
          </div>
        )}
        <form onSubmit={save}>
          <div className="form-grid">
            <div className="field">
              <label>Full name</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label>Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div className="field">
              <label>Phone</label>
              <input
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="+2507..."
              />
            </div>
            <div className="field" style={{ gridColumn: '1 / -1' }}>
              <label>Location</label>
              <LocationPicker
                value={form.locationId}
                onChange={(id) => setForm({ ...form, locationId: id || '' })}
              />
              {locationPath && (
                <small style={{ color: 'var(--ph-text-muted)' }}>
                  Province: <strong>{locationPath.provinceName || 'Unknown'}</strong>
                  {locationPath.districtName && <> - District: <strong>{locationPath.districtName}</strong></>}
                  {locationPath.sectorName && <> - Sector: <strong>{locationPath.sectorName}</strong></>}
                  {locationPath.cellName && <> - Cell: <strong>{locationPath.cellName}</strong></>}
                  {locationPath.villageName && <> - Village: <strong>{locationPath.villageName}</strong></>}
                </small>
              )}
            </div>
            <div className="field">
              <label>Role</label>
              <input value={form.role} readOnly />
            </div>
            <div className="field">
              <label>New password</label>
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Leave blank to keep current password"
              />
            </div>
          </div>
          <div style={{ marginTop: '1rem' }}>
            <PrimaryButton type="submit" disabled={saving}>
              {saving ? 'Saving...' : 'Save changes'}
            </PrimaryButton>
          </div>
        </form>
      </div>
    </div>
  );
}

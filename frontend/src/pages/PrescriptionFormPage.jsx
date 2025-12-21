import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import { useDashboard } from '../context/DashboardContext';
import Typeahead from '../components/Typeahead';

export default function PrescriptionFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { bumpDashboard } = useDashboard();

  const [selectedPatient, setSelectedPatient] = useState(null);
  const [selectedPharmacist, setSelectedPharmacist] = useState(null);
  const [anonymousPatient, setAnonymousPatient] = useState(false);

  const [form, setForm] = useState({
    patientId: '',
    pharmacistId: '',
    dateIssued: new Date().toISOString().slice(0, 16),
    status: 'PENDING',
    notes: '',
    walkInName: '',
    walkInPhone: '',
    items: [],
  });
  const [originalStatus, setOriginalStatus] = useState(null);
  const [error, setError] = useState('');

  const isEditing = !!id;
  const isCancelled = isEditing && originalStatus === 'CANCELLED';

  useEffect(() => {
    if (id) {
      const fetchPrescription = async () => {
        try {
          const res = await api.get(`/prescriptions/${id}`);
          const p = res.data;
          setForm({
            patientId: p.personId,
            pharmacistId: p.pharmacistId,
            dateIssued: p.dateIssued ? p.dateIssued.slice(0, 16) : '',
            status: p.status,
            notes: p.notes || '',
            walkInName: p.walkInName || '',
            walkInPhone: p.walkInPhone || '',
            items: (p.items || []).map(it => ({
              ...it,
              medicineId: it.medicineId,
              medicineName: it.medicineName,
            })),
          });
          setOriginalStatus(p.status);
          setSelectedPatient(
            p.personId
              ? { id: p.personId, name: p.patientName, email: p.patientEmail || '' }
              : null
          );
          setAnonymousPatient(!p.personId);
          setSelectedPharmacist(
            p.pharmacistId
              ? { id: p.pharmacistId, name: p.pharmacistName, email: p.pharmacistEmail || '' }
              : null
          );
        } catch (e) {
          setError('Failed to fetch prescription data.');
        }
      };
      fetchPrescription();
    }
  }, [id]);

  useEffect(() => {
    const loadPharmacist = async () => {
      if (isEditing) return;
      try {
        const res = await api.get('/persons/me');
        if (res.data?.id) {
          setSelectedPharmacist({
            id: res.data.id,
            name: res.data.name,
            email: res.data.email || '',
          });
          setForm((prev) => ({ ...prev, pharmacistId: res.data.id }));
        }
      } catch (e) {
        setError('Failed to load your pharmacist profile.');
      }
    };
    loadPharmacist();
  }, [isEditing]);

  const save = async () => {
    setError('');
    if (isCancelled) {
      setError('Cancelled prescriptions cannot be modified.');
      return;
    }
    if (!anonymousPatient && !form.patientId) {
      setError('Please select a patient.');
      return;
    }
    if (!form.pharmacistId) {
      setError('Please select a pharmacist.');
      return;
    }
    if (form.items.length === 0) {
      setError('Please add at least one medicine.');
      return;
    }
    const payload = {
      patientId: anonymousPatient ? undefined : form.patientId,
      pharmacistId: form.pharmacistId,
      notes: form.notes,
      walkInName: anonymousPatient ? (form.walkInName || undefined) : undefined,
      walkInPhone: anonymousPatient ? (form.walkInPhone || undefined) : undefined,
      status: isEditing ? form.status : 'PENDING', // New prescriptions always PENDING
      items: form.items.map((it) => ({
        medicineId: it.medicineId,
        quantity: Number(it.quantity || 1),
        dosage: it.dosage,
        frequency: it.frequency,
        duration: it.duration,
        instructions: it.instructions,
      })),
    };
    if (isEditing && form.dateIssued) {
      payload.dateIssued = `${form.dateIssued}:00`;
    }

    try {
      if (isEditing) {
        await api.put(`/prescriptions/${id}`, payload);
      } else {
        await api.post('/prescriptions', payload);
      }
      bumpDashboard();
      navigate('/prescriptions');
    } catch (e) {
      const data = e.response?.data;
      if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        const firstKey = Object.keys(data.fieldErrors)[0];
        setError(data.fieldErrors[firstKey]);
        return;
      }
      const msg =
        e.userMessage ||
        data?.message ||
        data?.error ||
        data ||
        'Failed to save prescription';
      setError(typeof msg === 'string' ? msg : 'Failed to save prescription');
    }
  };

  const addItemRow = () => {
    if (isCancelled) return;
    setForm({
      ...form,
      items: [
        ...form.items,
        {
          medicineId: '',
          medicineName: '',
          quantity: 1,
          dosage: '',
          frequency: '',
          duration: '',
          instructions: '',
        },
      ],
    });
  };

  const setItemSelection = (index, opt) => {
    const next = form.items.slice();
    next[index] = { ...next[index], medicineId: opt ? opt.id : '', medicineName: opt ? opt.name : '' };
    setForm({ ...form, items: next });
  };

  const updateItemRow = (index, field, value) => {
    const next = form.items.slice();
    next[index] = { ...next[index], [field]: value };
    setForm({ ...form, items: next });
  };

  const removeItemRow = (index) => {
    if (isCancelled) return;
    const next = form.items.slice();
    next.splice(index, 1);
    setForm({ ...form, items: next });
  };

  return (
    <div>
      <div className="section-title">
        {isEditing ? 'Edit Prescription' : 'New Prescription'}
      </div>
      <div className="card">
        <div className="form-grid">
          <div className="field">
            <label>Patient *</label>
            <Typeahead
              value={selectedPatient}
              onChange={(opt) => {
                setSelectedPatient(opt);
                setForm({ ...form, patientId: opt ? opt.id : '' });
                if (opt) {
                  setAnonymousPatient(false);
                }
              }}
              fetchOptions={async (term) => {
                const res = await api.get('/persons/search', { params: { q: term, role: 'PATIENT', page: 0, size: 10 } });
                return res.data.content || res.data || [];
              }}
              getOptionLabel={(o) => o ? (o.email ? `${o.name} (${o.email})` : o.name) : ''}
              placeholder="Type patient name..."
              disabled={anonymousPatient || isCancelled}
            />
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.8rem' }}>
              <input
                type="checkbox"
                checked={anonymousPatient}
                onChange={(e) => {
                  const checked = e.target.checked;
                  setAnonymousPatient(checked);
                  if (checked) {
                    setSelectedPatient(null);
                    setForm({ ...form, patientId: '' });
                  }
                }}
              />
              Walk-in (anonymous)
            </label>
            {anonymousPatient && (
              <div className="form-grid" style={{ marginTop: '0.5rem' }}>
                <div className="field">
                  <label>Customer name (optional)</label>
                  <input
                    value={form.walkInName}
                    onChange={(e) => setForm({ ...form, walkInName: e.target.value })}
                    placeholder="e.g. Tonku Badecon"
                    disabled={isCancelled}
                  />
                </div>
                <div className="field">
                  <label>Customer phone (optional)</label>
                  <input
                    value={form.walkInPhone}
                    onChange={(e) => setForm({ ...form, walkInPhone: e.target.value })}
                    placeholder="e.g. 0781234567"
                    disabled={isCancelled}
                  />
                </div>
              </div>
            )}
          </div>
          <div className="field">
            <label>Pharmacist *</label>
            <input
              value={
                selectedPharmacist
                  ? (selectedPharmacist.email
                    ? `${selectedPharmacist.name} (${selectedPharmacist.email})`
                    : selectedPharmacist.name)
                  : 'Logged-in user'
              }
              readOnly
              disabled
            />
            <small style={{ color: 'var(--ph-text-muted)' }}>
              Prescriptions are always issued under the logged-in account.
            </small>
          </div>
          <div className="field">
            <label>Date issued</label>
            <input
              type="datetime-local"
              value={form.dateIssued}
              readOnly
              disabled
            />
          </div>

          {/* Only show status on Edit - new prescriptions are always PENDING */}
          {isEditing && (
            <div className="field">
              <label>Status</label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
                disabled={form.status === 'FILLED' || isCancelled}
              >
                <option value="PENDING">PENDING</option>
                <option value="FILLED" disabled>FILLED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
              <small style={{ color: 'var(--ph-text-muted)' }}>
                {form.status === 'FILLED'
                  ? 'Status locked - prescription has been dispensed'
                  : 'Set to CANCELLED to void this prescription'}
              </small>
            </div>
          )}

          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <label>Notes</label>
            <textarea
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              placeholder="Clinical context, special instructions..."
              disabled={isCancelled}
            />
          </div>
        </div>

        {/* Items Section */}
        <div style={{ marginTop: '1rem' }}>
          <div className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span>Medicines</span>
            <PrimaryButton type="button" onClick={addItemRow} variant="secondary" disabled={isCancelled}>
              + Add Medicine
            </PrimaryButton>
          </div>

          {form.items.length === 0 ? (
            <div className="info" style={{ marginTop: '0.5rem' }}>
              No medicines added yet. Click "Add Medicine" to prescribe.
            </div>
          ) : (
            <div style={{ marginTop: '0.5rem' }}>
              {form.items.map((it, idx) => (
                <div
                  key={idx}
                  className="card"
                  style={{
                    marginBottom: '0.75rem',
                    background: 'var(--ph-bg-alt)',
                    border: it.medicineName ? '2px solid var(--ph-primary)' : '2px dashed var(--ph-border)'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <strong style={{ fontSize: '1.1rem', color: 'var(--ph-primary)' }}>
                      {it.medicineName || `Medicine #${idx + 1}`}
                    </strong>
                    <PrimaryButton
                      type="button"
                      variant="danger"
                      onClick={() => removeItemRow(idx)}
                      disabled={isCancelled}
                    >
                      Remove
                    </PrimaryButton>
                  </div>

                  <div className="form-grid">
                    <div className="field">
                      <label>Medicine *</label>
                      <Typeahead
                        value={it.medicineId ? { id: it.medicineId, name: it.medicineName } : null}
                        onChange={(opt) => setItemSelection(idx, opt)}
                        fetchOptions={async (term) => {
                          const res = await api.get('/medicines', { params: { q: term, page: 0, size: 10 } });
                          return res.data.content || res.data || [];
                        }}
                        getOptionLabel={(o) => o ? o.name : ''}
                        placeholder="Search medicine..."
                        disabled={isCancelled}
                      />
                    </div>
                    <div className="field">
                      <label>Quantity *</label>
                      <input
                        type="number"
                        min="1"
                        value={it.quantity}
                        onChange={(e) => updateItemRow(idx, 'quantity', e.target.value)}
                        style={{ width: '80px' }}
                        disabled={isCancelled}
                      />
                    </div>
                    <div className="field">
                      <label>Dosage</label>
                      <input
                        value={it.dosage || ''}
                        onChange={(e) => updateItemRow(idx, 'dosage', e.target.value)}
                        placeholder="e.g. 500mg"
                        disabled={isCancelled}
                      />
                    </div>
                    <div className="field">
                      <label>Frequency</label>
                      <input
                        value={it.frequency || ''}
                        onChange={(e) => updateItemRow(idx, 'frequency', e.target.value)}
                        placeholder="e.g. 3x daily"
                        disabled={isCancelled}
                      />
                    </div>
                    <div className="field">
                      <label>Duration</label>
                      <input
                        value={it.duration || ''}
                        onChange={(e) => updateItemRow(idx, 'duration', e.target.value)}
                        placeholder="e.g. 7 days"
                        disabled={isCancelled}
                      />
                    </div>
                    <div className="field" style={{ gridColumn: '1 / -1' }}>
                      <label>Instructions</label>
                      <input
                        value={it.instructions || ''}
                        onChange={(e) => updateItemRow(idx, 'instructions', e.target.value)}
                        placeholder="e.g. Take after meals"
                        disabled={isCancelled}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {error && (
            <div className="error" style={{ marginTop: '0.5rem' }}>
              {error}
            </div>
          )}
        </div>

        <div style={{ marginTop: '1rem' }}>
          <PrimaryButton type="button" onClick={save} disabled={isCancelled}>
            {isEditing ? 'Update Prescription' : 'Create Prescription'}
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../api';

export default function PrescriptionDetailPage() {
  const { id } = useParams();
  const [details, setDetails] = useState(null);
  const [error, setError] = useState('');
  const [fillItems, setFillItems] = useState([]);
  const [fillMessage, setFillMessage] = useState('');
  const [billInfo, setBillInfo] = useState(null);
  const [billError, setBillError] = useState('');

  useEffect(() => {
    const fetchDetails = async () => {
      try {
        const res = await api.get(`/prescriptions/${id}`);
        setDetails(res.data);
        setFillItems(
          (res.data.items || []).map((it) => ({
            id: it.id,
            medicineName: it.medicineName,
            prescribedQty: it.quantity,
            dispenseQty: it.quantity,
          }))
        );
      } catch (e) {
        setError('Failed to fetch prescription details.');
      }
    };
    fetchDetails();
  }, [id]);

  const onDispense = async () => {
    setFillMessage('');
    setError('');
    try {
      const payload = {
        items: fillItems
          .filter((it) => Number(it.dispenseQty) > 0)
          .map((it) => ({
            prescriptionItemId: it.id,
            quantity: Number(it.dispenseQty),
          })),
      };
      const res = await api.post(`/prescriptions/${id}/fill`, payload);
      setDetails(res.data);
      setFillMessage('Dispensed successfully. Stock updated and status refreshed.');
    } catch (e) {
      setError(e?.response?.data?.message || e?.userMessage || 'Dispense failed.');
    }
  };

  const fetchBill = async () => {
    setBillError('');
    setBillInfo(null);
    try {
      const res = await api.get(`/bills/by-prescription/${id}`);
      setBillInfo(res.data);
    } catch (e) {
      setBillError(e?.response?.data?.message || 'No bill yet for this prescription.');
    }
  };

  const createBill = async () => {
    setBillError('');
    setBillInfo(null);
    try {
      const now = new Date().toISOString().slice(0, 19);
      const res = await api.post(`/bills/for-prescription/${id}`, {
        status: 'PENDING',
        paymentMethod: null,
        paymentRef: null,
        discountAmount: 0,
        taxAmount: 0,
        paymentDate: now,
      });
      setBillInfo(res.data);
      setFillMessage('Bill created from dispensed items.');
    } catch (e) {
      setBillError(e?.response?.data?.message || 'Failed to create bill.');
    }
  };

  if (error) {
    return <div className="error">{error}</div>;
  }

  if (!details) {
    return <div>Loading...</div>;
  }

  return (
    <div>
      <div className="section-title">Prescription #{details.id}</div>
      <div className="card">
        <div className="form-grid">
          <div className="field">
            <label>Patient</label>
            <input value={details.patientName || `Patient #${details.personId}`} readOnly />
          </div>
          <div className="field">
            <label>Pharmacist</label>
            <input value={details.pharmacistName || `Pharmacist #${details.pharmacistId}`} readOnly />
          </div>
          <div className="field">
            <label>Status</label>
            <input
              value={
                details.status === 'PENDING'
                  ? 'Pending (not yet dispensed)'
                  : details.status === 'FILLED'
                    ? 'Filled (medicines dispensed)'
                    : 'Cancelled'
              }
              readOnly
            />
          </div>
          <div className="field">
            <label>Date issued</label>
            <input value={details.dateIssued} readOnly />
          </div>
          <div className="field" style={{ gridColumn: '1 / -1' }}>
            <label>Notes</label>
            <textarea value={details.notes || ''} readOnly />
          </div>
        </div>
        <div style={{ marginTop: '0.75rem' }}>
          <div className="section-title">Items</div>
          <table>
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Qty</th>
                <th>Dispense</th>
                <th>Dosage</th>
                <th>Frequency</th>
                <th>Duration</th>
                <th>Instructions</th>
              </tr>
            </thead>
            <tbody>
              {details.items?.map((it) => (
                <tr key={it.id}>
                  <td>{it.medicineName}</td>
                  <td>{it.quantity}</td>
                  <td>
                    {details.status === 'PENDING' ? (
                      <input
                        type="number"
                        min="0"
                        max={it.quantity}
                        value={
                          fillItems.find((f) => f.id === it.id)?.dispenseQty ?? it.quantity
                        }
                        onChange={(e) => {
                          const next = fillItems.map((f) =>
                            f.id === it.id ? { ...f, dispenseQty: e.target.value } : f
                          );
                          setFillItems(next);
                        }}
                        style={{ width: '80px' }}
                      />
                    ) : (
                      it.dispensedQuantity ?? it.quantity
                    )}
                  </td>
                  <td>{it.dosage}</td>
                  <td>{it.frequency}</td>
                  <td>{it.duration}</td>
                  <td>{it.instructions}</td>
                </tr>
              ))}
              {(!details.items || details.items.length === 0) && (
                <tr>
                  <td colSpan={6}>No items on this prescription.</td>
                </tr>
              )}
            </tbody>
          </table>
          {details.status === 'PENDING' && (
            <div style={{ marginTop: '0.75rem' }}>
              <button type="button" className="btn btn-primary" onClick={onDispense}>
                Dispense prescription
              </button>
              {fillMessage && (
                <div className="info" style={{ marginTop: '0.5rem' }}>
                  {fillMessage}
                </div>
              )}
            </div>
          )}
          <div style={{ marginTop: '0.75rem' }}>
            <button type="button" className="btn btn-secondary" onClick={fetchBill}>
              View bill
            </button>{' '}
            <button type="button" className="btn btn-primary" onClick={createBill}>
              Create bill
            </button>
            {billError && (
              <div className="error" style={{ marginTop: '0.5rem' }}>
                {billError}
              </div>
            )}
            {billInfo && (
              <div className="card" style={{ marginTop: '0.5rem' }}>
                <div><strong>Bill #{billInfo.id}</strong></div>
                <div>Status: {billInfo.status}</div>
                <div>Payment method: {billInfo.paymentMethod || 'None'}</div>
                <div>Subtotal: {billInfo.subtotal}</div>
                <div>Net total: {billInfo.netTotal}</div>
                <div>Date: {billInfo.paymentDate}</div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';

/**
 * Prescription detail page with dispensing workflow.
 */
export default function PrescriptionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();

  const [rx, setRx] = useState(null);
  const [dispenseQty, setDispenseQty] = useState({});
  const [message, setMessage] = useState({ type: '', title: '', text: '' });
  const [bill, setBill] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showPayment, setShowPayment] = useState(false);
  const [paymentForm, setPaymentForm] = useState({
    paymentMethod: 'CASH',
    paymentRef: '',
    discountAmount: 0,
    taxAmount: 0
  });

  useEffect(() => {
    loadPrescription();
  }, [id]);

  const loadPrescription = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/prescriptions/${id}`);
      setRx(res.data);

      // Initialize dispense quantities
      const qty = {};
      (res.data.items || []).forEach(item => {
        const remaining = (item.quantity || 0) - (item.dispensedQuantity || 0);
        qty[item.id] = Math.max(0, remaining);
      });
      setDispenseQty(qty);

      // Only load bill if prescription has dispensed items
      const hasDispensed = res.data.items?.some(i => i.dispensedQuantity > 0);
      if (hasDispensed) {
        loadBill();
      }
    } catch (e) {
      setMessage(formatError('Load failed', e, 'Failed to load prescription'));
    } finally {
      setLoading(false);
    }
  };

  const loadBill = async () => {
    try {
      const res = await api.get(`/bills/by-prescription/${id}`);
      setBill(res.data);
    } catch (e) {
      // No bill yet - that's fine
    }
  };

  const handleDispense = async () => {
    setMessage({ type: '', title: '', text: '' });

    const items = Object.entries(dispenseQty)
      .filter(([_, qty]) => qty > 0)
      .map(([itemId, qty]) => ({
        prescriptionItemId: Number(itemId),
        quantity: qty
      }));

    if (items.length === 0) {
      setMessage({
        type: 'error',
        title: 'Dispense error',
        text: 'Enter quantities to dispense before submitting.',
      });
      return;
    }

    try {
      const res = await api.post(`/prescriptions/${id}/fill`, { items });
      setRx(res.data);

      // Reset dispense quantities
      const qty = {};
      (res.data.items || []).forEach(item => {
        const remaining = (item.quantity || 0) - (item.dispensedQuantity || 0);
        qty[item.id] = Math.max(0, remaining);
      });
      setDispenseQty(qty);

      setMessage({ type: 'success', title: 'Success', text: 'Dispensed successfully.' });
      loadBill();
    } catch (e) {
      setMessage(formatError('Dispense failed', e, 'Dispense failed'));
    }
  };

  const handlePayment = async () => {
    setMessage({ type: '', title: '', text: '' });
    if (!paymentForm.paymentRef?.trim()) {
      setMessage({
        type: 'error',
        title: 'Payment error',
        text: 'Payment reference is required for this payment.',
      });
      return;
    }
    // Validate phone number for Mobile Money
    if (paymentForm.paymentMethod === 'MOBILE') {
      const phone = paymentForm.paymentRef?.replace(/\s/g, '');
      if (!phone || !/^07\d{8}$/.test(phone)) {
        setMessage({
          type: 'error',
          title: 'Payment error',
          text: 'Mobile Money requires a Rwanda phone number: 10 digits starting with 07 (e.g. 0781234567).',
        });
        return;
      }
    }

    // Validate reference for Card
    if (paymentForm.paymentMethod === 'CARD' && !paymentForm.paymentRef?.trim()) {
      setMessage({
        type: 'error',
        title: 'Payment error',
        text: 'Card payments require a reference (last 4 digits or auth code).',
      });
      return;
    }

    try {
      await api.put(`/bills/${bill.id}`, {
        status: 'PAID',
        paymentMethod: paymentForm.paymentMethod,
        paymentRef: paymentForm.paymentRef || null,
        discountAmount: 0,
        taxAmount: 0
      });

      setShowPayment(false);
      // Navigate to Bills page with the bill focused for printing
      navigate(`/bills?focus=${bill.id}`);
    } catch (e) {
      setMessage(formatError('Payment failed', e, 'Payment failed'));
    }
  };

  const formatError = (title, error, fallback) => {
    const data = error?.response?.data;
    if (data?.fieldErrors && typeof data.fieldErrors === 'object') {
      const firstKey = Object.keys(data.fieldErrors)[0];
      const fieldMessage = data.fieldErrors[firstKey];
      return {
        type: 'error',
        title,
        text: `${firstKey}: ${fieldMessage}`,
      };
    }
    const serverMessage =
      data?.message ||
      data?.error ||
      (typeof data === 'string' ? data : null) ||
      error?.message;
    return {
      type: 'error',
      title,
      text: serverMessage || fallback,
    };
  };

  if (loading) return <div>Loading...</div>;
  if (!rx) return <div className="error">Prescription not found</div>;

  const isPharmacist = role === 'PHARMACIST' || role === 'ADMIN';
  const isPaid = bill?.status === 'PAID';
  const hasDispensed = rx.items?.some(i => (i.dispensedQuantity || 0) > 0);
  const isCancelled = rx.status === 'CANCELLED';
  const formatWalkInLabel = (patientName, walkInName, walkInPhone) => {
    const name = walkInName ? walkInName.trim() : '';
    const phone = walkInPhone ? walkInPhone.trim() : '';
    if (name || phone) {
      return `Walk-in: ${name || phone}`;
    }
    if (patientName && patientName.toLowerCase() === 'walk-in customer') {
      return 'Walk-in Customer';
    }
    return patientName || '-';
  };

  // Calculate if ALL items are fully dispensed (don't trust status, calculate from data)
  const allFullyDispensed = rx.items?.length > 0 && rx.items.every(i => {
    const prescribed = i.quantity || 0;
    const dispensed = i.dispensedQuantity || 0;
    return prescribed > 0 && dispensed >= prescribed;
  });

  // Use calculated value for completion, but respect explicit cancellation.
  const isComplete = allFullyDispensed;
  const statusLabel = isCancelled ? 'Cancelled' : isComplete ? 'Filled' : 'Pending';
  const statusClass = isCancelled ? 'status-pill--danger' : isComplete ? 'status-pill--ok' : 'status-pill--warn';

  return (
    <div>
      {/* Back button */}
      <div style={{ marginBottom: '1rem' }}>
        <PrimaryButton variant="secondary" onClick={() => navigate('/prescriptions')}>
          ← Back to Prescriptions
        </PrimaryButton>
      </div>

      {/* Header - inline title and status */}
      <div className="card" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '1rem 1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <h1 style={{ margin: 0, fontSize: '1.5rem' }}>Prescription #{rx.id}</h1>
          <span className={`status-pill ${statusClass}`}>
            {statusLabel}
          </span>
        </div>
        {message.text && (
          <div className={message.type === 'error' ? 'error' : 'info'} style={{ margin: 0, padding: '0.5rem 1rem' }}>
            {message.title && (
              <div style={{ fontWeight: 600, marginBottom: '0.25rem' }}>
                {message.title}
              </div>
            )}
            <div>{message.text}</div>
          </div>
        )}
      </div>

      {/* Prescription Info */}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div className="form-grid">
          <div className="field">
            <label>Patient</label>
            <input value={formatWalkInLabel(rx.patientName, rx.walkInName, rx.walkInPhone)} readOnly />
          </div>
          <div className="field">
            <label>Pharmacist</label>
            <input value={rx.pharmacistName || '-'} readOnly />
          </div>
          <div className="field">
            <label>Date Issued</label>
            <input value={rx.dateIssued?.split('T')[0] || '-'} readOnly />
          </div>
          <div className="field">
            <label>Notes</label>
            <input value={rx.notes || '-'} readOnly />
          </div>
        </div>
      </div>

      {/* Items Table */}
      <div className="card">
        <div className="section-title">Prescription Items</div>
        <table>
          <thead>
            <tr>
              <th>Medicine</th>
              <th>Prescribed</th>
              <th>Dispensed</th>
              <th>Remaining</th>
              {isPharmacist && !isComplete && !isPaid && !isCancelled && <th>Dispense Now</th>}
              <th>Dosage</th>
              <th>Frequency</th>
            </tr>
          </thead>
          <tbody>
            {rx.items?.map(item => {
              const prescribed = item.quantity || 0;
              const dispensed = item.dispensedQuantity || 0;
              const remaining = Math.max(0, prescribed - dispensed);
              const complete = dispensed >= prescribed;

              return (
                <tr key={item.id}>
                  <td><strong>{item.medicineName}</strong></td>
                  <td>{prescribed}</td>
                  <td>
                    <span className={complete ? 'status-pill status-pill--ok' : ''}>
                      {dispensed}
                    </span>
                  </td>
                  <td>{complete ? '✓' : remaining}</td>
                  {isPharmacist && !isComplete && !isPaid && !isCancelled && (
                    <td>
                      {complete ? (
                        <span className="chip">Done</span>
                      ) : (
                        <input
                          type="number"
                          min="0"
                          max={remaining}
                          value={dispenseQty[item.id] || 0}
                          onChange={e => setDispenseQty({
                            ...dispenseQty,
                            [item.id]: Math.min(Number(e.target.value), remaining)
                          })}
                          style={{ width: '70px' }}
                        />
                      )}
                    </td>
                  )}
                  <td>{item.dosage || '-'}</td>
                  <td>{item.frequency || '-'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {/* Dispense Button */}
        {isCancelled && (
          <div className="info" style={{ marginTop: '1rem' }}>
            This prescription is cancelled and cannot be dispensed or billed.
          </div>
        )}

        {isPharmacist && !isComplete && !isPaid && !isCancelled && (
          <div style={{ marginTop: '1rem' }}>
            <PrimaryButton onClick={handleDispense}>
              Dispense Selected Items
            </PrimaryButton>
          </div>
        )}

        {isComplete && (
          <div className="info" style={{ marginTop: '1rem' }}>
            ✓ All items dispensed
          </div>
        )}
      </div>

      {/* Bill Section - only show if something has been dispensed */}
      {hasDispensed && !isCancelled && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <div className="section-title">Bill</div>

          {bill ? (
            <div>
              <div className="form-grid">
                <div className="field">
                  <label>Bill ID</label>
                  <input value={`#${bill.id}`} readOnly />
                </div>
                <div className="field">
                  <label>Status</label>
                  <span className={`status-pill ${bill.status === 'PAID' ? 'status-pill--ok' : 'status-pill--warn'
                    }`} style={{ marginLeft: '0.5rem' }}>
                    {bill.status}
                  </span>
                </div>
                <div className="field">
                  <label>Subtotal</label>
                  <input value={`${bill.subtotal || 0} ${bill.currency}`} readOnly />
                </div>
                <div className="field">
                  <label>Net Total</label>
                  <input value={`${bill.netTotal || 0} ${bill.currency}`} readOnly style={{ fontWeight: 'bold' }} />
                </div>
              </div>

              {bill.status !== 'PAID' && isComplete && (
                <div style={{ marginTop: '1rem' }}>
                  <PrimaryButton onClick={() => setShowPayment(true)}>
                    Process Payment
                  </PrimaryButton>
                </div>
              )}

              {bill.status === 'PAID' && (
                <div className="info" style={{ marginTop: '1rem' }}>
                  ✓ Paid via {bill.paymentMethod} {bill.paymentRef ? `(Ref: ${bill.paymentRef})` : ''}
                </div>
              )}
            </div>
          ) : (
            <PrimaryButton variant="secondary" onClick={loadBill}>
              Load Bill
            </PrimaryButton>
          )}
        </div>
      )}

      {!hasDispensed && !isCancelled && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <div className="section-title">Bill</div>
          <p style={{ color: 'var(--ph-text-muted)' }}>
            Bill will be generated after dispensing items.
          </p>
        </div>
      )}

      {/* Payment Modal */}
      {showPayment && (
        <Modal
          title="Process Payment"
          onClose={() => setShowPayment(false)}
          primaryLabel="Confirm Payment"
          onPrimary={handlePayment}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            <div className="field">
              <label>Amount Due</label>
              <input value={`${bill?.subtotal || 0} ${bill?.currency}`} readOnly style={{ fontWeight: 'bold', fontSize: '1.2rem', color: 'var(--ph-primary)' }} />
            </div>
            <div className="field">
              <label>Payment Method *</label>
              <select
                value={paymentForm.paymentMethod}
                onChange={e => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}
              >
                <option value="CASH">💵 Cash</option>
                <option value="CARD">💳 Card</option>
                <option value="MOBILE">📱 Mobile Money</option>
              </select>
              <small style={{ color: 'var(--ph-text-muted)', marginTop: '0.25rem' }}>
                {paymentForm.paymentMethod === 'CASH' && 'Customer pays with physical cash (reference required)'}
                {paymentForm.paymentMethod === 'CARD' && 'Credit/Debit card payment'}
                {paymentForm.paymentMethod === 'MOBILE' && 'MTN MoMo, Airtel Money, etc.'}
              </small>
            </div>
            <div className="field">
              <label>
                Reference
                <span style={{ color: 'var(--ph-danger)' }}> *</span>
              </label>
              <input
                value={paymentForm.paymentRef}
                onChange={e => setPaymentForm({ ...paymentForm, paymentRef: e.target.value })}
                placeholder={
                  paymentForm.paymentMethod === 'CASH' ? 'Receipt number' :
                    paymentForm.paymentMethod === 'CARD' ? 'Last 4 digits or auth code' :
                      'Transaction ID or phone number'
                }
              />
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

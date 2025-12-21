import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';

/**
 * Bill receipt page - printable invoice for a paid bill
 */
export default function BillReceiptPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [bill, setBill] = useState(null);
    const [prescription, setPrescription] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        loadData();
    }, [id]);

    const loadData = async () => {
        try {
            setLoading(true);
            const billRes = await api.get(`/bills/${id}`);
            setBill(billRes.data);

            if (billRes.data.prescriptionId) {
                const rxRes = await api.get(`/prescriptions/${billRes.data.prescriptionId}`);
                setPrescription(rxRes.data);
            }
        } catch (e) {
            setError('Failed to load bill');
        } finally {
            setLoading(false);
        }
    };

    const handlePrint = () => {
        window.print();
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div className="error">{error}</div>;
    if (!bill) return <div className="error">Bill not found</div>;

    return (
        <div>
            {/* No-print header */}
            <div className="no-print" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem' }}>
                <PrimaryButton onClick={() => navigate('/bills')}>
                    ← Back to Bills
                </PrimaryButton>
                <PrimaryButton onClick={handlePrint}>
                    🖨️ Print Receipt
                </PrimaryButton>
            </div>

            {/* Printable receipt */}
            <div className="receipt" style={{
                maxWidth: '600px',
                margin: '0 auto',
                padding: '2rem',
                background: 'white',
                border: '1px solid #ddd',
                fontFamily: 'Georgia, serif'
            }}>
                {/* Header */}
                <div style={{ textAlign: 'center', marginBottom: '2rem', borderBottom: '2px solid #10b981', paddingBottom: '1rem' }}>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: '#10b981', letterSpacing: '2px' }}>💊 BDCare</div>
                    <p style={{ margin: '0.25rem 0 0', color: '#666', fontSize: '0.9rem' }}>Pharmacy Management System</p>
                    <h2 style={{ margin: '1rem 0 0', fontSize: '1.2rem', letterSpacing: '1px', color: '#064e3b' }}>OFFICIAL RECEIPT</h2>
                </div>

                {/* Bill Info */}
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
                    <div>
                        <strong>Receipt #:</strong> {bill.id}<br />
                        <strong>Date:</strong> {bill.paymentDate?.split('T')[0] || '-'}
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <strong>Status:</strong> <span style={{ color: bill.status === 'PAID' ? 'green' : 'orange' }}>{bill.status}</span><br />
                        <strong>Payment:</strong> {bill.paymentMethod || '-'}
                    </div>
                </div>

                {/* Customer/Patient Info */}
                {(() => {
                    const walkInName = bill.customerName ? bill.customerName.trim() : '';
                    const walkInPhone = bill.customerPhone ? bill.customerPhone.trim() : '';
                    const hasWalkIn = walkInName || walkInPhone;
                    const patientName = bill.patientName ? bill.patientName.trim() : '';
                    const isWalkInLabel = patientName.toLowerCase() === 'walk-in customer';

                    if (hasWalkIn) {
                        return (
                            <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f9f9f9', borderRadius: '4px' }}>
                                <strong>Customer:</strong> {walkInName || 'N/A'}<br />
                                {walkInPhone && (
                                    <>
                                        <strong>Phone:</strong> {walkInPhone}<br />
                                    </>
                                )}
                                <strong>Prescription #:</strong> {bill.prescriptionId || '-'}
                            </div>
                        );
                    }

                    if (patientName && !isWalkInLabel) {
                        return (
                            <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f9f9f9', borderRadius: '4px' }}>
                                <strong>Patient:</strong> {patientName}<br />
                                <strong>Prescription #:</strong> {bill.prescriptionId || '-'}
                            </div>
                        );
                    }

                    return (
                        <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f9f9f9', borderRadius: '4px' }}>
                            <strong>Prescription #:</strong> {bill.prescriptionId || '-'}
                        </div>
                    );
                })()}

                {/* Items */}
                {prescription?.items && prescription.items.length > 0 && (
                    <div style={{ marginBottom: '1.5rem' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ borderBottom: '2px solid #333' }}>
                                    <th style={{ textAlign: 'left', padding: '0.5rem 0' }}>Item</th>
                                    <th style={{ textAlign: 'center', padding: '0.5rem 0' }}>Qty</th>
                                    <th style={{ textAlign: 'right', padding: '0.5rem 0' }}>Unit</th>
                                    <th style={{ textAlign: 'right', padding: '0.5rem 0' }}>Amount</th>
                                </tr>
                            </thead>
                            <tbody>
                                {prescription.items.filter(i => i.dispensedQuantity > 0).map((item, idx) => {
                                    const unitPrice = item.medicinePrice || 0;
                                    const lineTotal = item.dispensedQuantity * unitPrice;
                                    return (
                                        <tr key={idx} style={{ borderBottom: '1px solid #ddd' }}>
                                            <td style={{ padding: '0.5rem 0' }}>{item.medicineName}</td>
                                            <td style={{ textAlign: 'center', padding: '0.5rem 0' }}>{item.dispensedQuantity}</td>
                                            <td style={{ textAlign: 'right', padding: '0.5rem 0' }}>{unitPrice.toLocaleString()}</td>
                                            <td style={{ textAlign: 'right', padding: '0.5rem 0', fontWeight: '500' }}>{lineTotal.toLocaleString()}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Totals */}
                <div style={{ borderTop: '2px solid #333', paddingTop: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <span>Subtotal:</span>
                        <span>{bill.subtotal} {bill.currency}</span>
                    </div>
                    {bill.discountAmount > 0 && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', color: 'green' }}>
                            <span>Discount:</span>
                            <span>-{bill.discountAmount} {bill.currency}</span>
                        </div>
                    )}
                    {bill.taxAmount > 0 && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                            <span>Tax:</span>
                            <span>+{bill.taxAmount} {bill.currency}</span>
                        </div>
                    )}
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', fontSize: '1.2rem', marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid #ddd' }}>
                        <span>TOTAL:</span>
                        <span>{bill.netTotal} {bill.currency}</span>
                    </div>
                </div>

                {/* Payment Ref */}
                {bill.paymentRef && (
                    <div style={{ marginTop: '1rem', fontSize: '0.9rem', color: '#666' }}>
                        <strong>Reference:</strong> {bill.paymentRef}
                    </div>
                )}

                {/* Footer */}
                <div style={{ marginTop: '2rem', textAlign: 'center', fontSize: '0.85rem', color: '#666', borderTop: '1px dashed #10b981', paddingTop: '1rem' }}>
                    <strong style={{ color: '#10b981' }}>Thank you for choosing BDCare!</strong><br />
                    <small>Your health, our priority.</small><br /><br />
                    <small style={{ color: '#999' }}>
                        BDCare Pharmacy Management System<br />
                        Kigali, Rwanda | support@bdcare.rw<br />
                        This is an official computer-generated receipt.
                    </small>
                </div>
            </div>

            {/* Print styles */}
            <style>{`
        @media print {
          .no-print { display: none !important; }
          .receipt { border: none !important; box-shadow: none !important; }
          body { background: white !important; }
          nav, header, footer, .sidebar { display: none !important; }
        }
      `}</style>
        </div>
    );
}

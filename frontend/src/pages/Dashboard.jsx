import { useEffect, useState } from 'react';
import api from '../api';
import { useDashboard } from '../context/DashboardContext';
import { useAuth } from '../context/AuthContext';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [lowStock, setLowStock] = useState([]);
  const [error, setError] = useState('');
  const { version } = useDashboard();
  const { role } = useAuth();

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/dashboard/summary');
        setData(res.data);
      } catch (e) {
        setError(
          'Unable to load dashboard summary. You might not have access, or the backend summary endpoint failed.'
        );
      }
      if (role === 'ADMIN') {
        try {
          const lowRes = await api.get('/medicines/low-stock');
          setLowStock(lowRes.data || []);
        } catch {
          // ignore low-stock error, keep rest of dashboard
        }
      } else {
        setLowStock([]);
      }
    };
    load();
  }, [version, role]);

  if (!data && !error) return <div>Loading dashboard...</div>;

  const isPatient = role === 'PATIENT';
  const isPharmacist = role === 'PHARMACIST';
  const isAdmin = role === 'ADMIN';

  const heroTitle = isAdmin
    ? 'Operations Control'
    : isPharmacist
      ? 'Shift Snapshot'
      : 'Welcome Back';
  const heroSubtitle = isAdmin
    ? 'System pulse and throughput at a glance.'
    : isPharmacist
      ? 'Your prescriptions and billing activity.'
      : 'Your prescriptions and billing activity.';

  const formatDate = (value) => {
    if (!value) return '-';
    return String(value).split('T')[0];
  };

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

  const cardConfigs = isPatient
    ? [
      { label: 'My Prescriptions', value: data?.totalPrescriptions ?? 'N/A', icon: 'Rx', tone: 'emerald' },
      { label: 'Pending', value: data?.pendingPrescriptions ?? 'N/A', icon: 'P', tone: 'amber' },
      { label: 'Filled', value: data?.filledPrescriptions ?? 'N/A', icon: 'OK', tone: 'emerald' },
      { label: 'My Bills', value: data?.totalBills ?? 'N/A', icon: 'Bill', tone: 'cyan' },
      { label: 'Paid Bills', value: data?.paidBills ?? 'N/A', icon: 'OK', tone: 'emerald' },
      { label: 'Total Paid', value: data?.totalRevenue != null ? `${data.totalRevenue.toLocaleString()} RWF` : 'N/A', icon: '$', tone: 'emerald' },
    ]
    : isPharmacist
      ? [
        { label: 'My Prescriptions', value: data?.totalPrescriptions ?? 'N/A', icon: 'Rx', tone: 'emerald' },
        { label: 'Pending', value: data?.pendingPrescriptions ?? 'N/A', icon: 'P', tone: 'amber' },
        { label: 'Filled', value: data?.filledPrescriptions ?? 'N/A', icon: 'OK', tone: 'emerald' },
        { label: 'My Bills', value: data?.totalBills ?? 'N/A', icon: 'Bill', tone: 'cyan' },
        { label: 'Paid Bills', value: data?.paidBills ?? 'N/A', icon: 'OK', tone: 'emerald' },
        { label: 'Revenue (PAID)', value: data?.totalRevenue != null ? `${data.totalRevenue.toLocaleString()} RWF` : 'N/A', icon: '$', tone: 'emerald' },
      ]
      : [
        { label: 'Patients', value: data?.totalPatients ?? 'N/A', icon: 'Pts', tone: 'cyan' },
        { label: 'Pharmacists', value: data?.totalPharmacists ?? 'N/A', icon: 'Pharm', tone: 'emerald' },
        { label: 'Medicines', value: data?.totalMedicines ?? 'N/A', icon: 'Med', tone: 'indigo' },
        { label: 'Low Stock', value: data?.lowStockMedicines ?? lowStock.length, icon: 'Low', tone: 'amber' },
        { label: 'Prescriptions', value: data?.totalPrescriptions ?? 'N/A', icon: 'Rx', tone: 'cyan' },
        { label: 'Revenue (PAID)', value: data?.totalRevenue != null ? `${data.totalRevenue.toLocaleString()} RWF` : 'N/A', icon: '$', tone: 'emerald' },
      ];

  return (
    <div className="dash-wrap">
      <section className="dash-hero">
        <div className="dash-hero__content">
          <div className="dash-hero__eyebrow">Dashboard</div>
          <h1 className="dash-hero__title">{heroTitle}</h1>
          <p className="dash-hero__subtitle">{heroSubtitle}</p>
        </div>
        <div className="dash-hero__meta">
          <div className="dash-chip">Role: {role || 'UNKNOWN'}</div>
          <div className="dash-chip">View: {isAdmin ? 'System' : 'My Activity'}</div>
          <div className="dash-chip">Updated: {new Date().toLocaleDateString()}</div>
        </div>
      </section>

      {error && (
        <div className="dash-error">
          {error}
        </div>
      )}

      <section className="dash-kpis">
        {cardConfigs.map((c, idx) => (
          <div className={`dash-kpi dash-kpi--${c.tone}`} key={`${c.label}-${idx}`}>
            <div className="dash-kpi__top">
              <span className="dash-kpi__label">{c.label}</span>
              <span className="dash-kpi__icon">{c.icon}</span>
            </div>
            <div className="dash-kpi__value">{c.value}</div>
          </div>
        ))}
      </section>

      <section className="dash-grid">
        <div className="dash-panel dash-panel--wide">
          <div className="dash-panel__header">
            <div>
              <h3>{isPatient || isPharmacist ? 'My recent prescriptions' : 'Recent prescriptions'}</h3>
              <p>Track status changes and dispenses in real time.</p>
            </div>
          </div>
          <table className="dash-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Patient</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {data?.recentPrescriptions?.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{formatWalkInLabel(p.patientName, p.walkInName, p.walkInPhone)}</td>
                  <td>
                    <span
                      className={
                        'status-pill ' +
                        (p.status === 'PENDING'
                          ? 'status-pill--warn'
                          : p.status === 'FILLED'
                            ? 'status-pill--ok'
                            : 'status-pill--danger')
                      }
                    >
                      {p.status}
                    </span>
                  </td>
                  <td>{formatDate(p.dateIssued)}</td>
                </tr>
              ))}
              {(!data?.recentPrescriptions || data.recentPrescriptions.length === 0) && (
                <tr>
                  <td colSpan={4}>No prescriptions yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="dash-panel">
          <div className="dash-panel__header">
            <div>
              <h3>{isPatient || isPharmacist ? 'My recent bills' : 'Recent bills'}</h3>
              <p>Recent payments and pending balances.</p>
            </div>
          </div>
          <table className="dash-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Rx</th>
                <th>Amount</th>
                <th>Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data?.recentBills?.map((b) => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td>{b.prescriptionId}</td>
                  <td>{b.netTotal}</td>
                  <td>{formatDate(b.paymentDate)}</td>
                  <td>
                    <span
                      className={
                        'status-pill ' +
                        (b.status === 'PAID'
                          ? 'status-pill--ok'
                          : b.status === 'PENDING'
                            ? 'status-pill--warn'
                            : 'status-pill--danger')
                      }
                    >
                      {b.status}
                    </span>
                  </td>
                </tr>
              ))}
              {(!data?.recentBills || data.recentBills.length === 0) && (
                <tr>
                  <td colSpan={5}>No recent bills yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {isAdmin && (
          <div className="dash-panel dash-panel--low">
            <div className="dash-panel__header">
              <div>
                <h3>Low stock medicines</h3>
                <p>Based on each medicine's own reorder level (fallback threshold 10).</p>
              </div>
            </div>
            {lowStock.length === 0 && (
              <div className="dash-empty">No medicines are currently below the threshold.</div>
            )}
            {lowStock.length > 0 && (
              <table className="dash-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Current</th>
                    <th>Reorder</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStock.map((m) => (
                    <tr key={m.id}>
                      <td>{m.name}</td>
                      <td>{m.quantity}</td>
                      <td>{m.reorderLevel ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

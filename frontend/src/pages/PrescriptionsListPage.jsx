import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import { useDashboard } from '../context/DashboardContext';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';
import { useAuth } from '../context/AuthContext';

export default function PrescriptionsListPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);

  // Individual filter fields (strings work better with useDebounce)
  const [statusFilter, setStatusFilter] = useState('');
  const [patientNameFilter, setPatientNameFilter] = useState('');
  const [pharmacistNameFilter, setPharmacistNameFilter] = useState('');
  const [idFilter, setIdFilter] = useState('');
  const [tableSearch, setTableSearch] = useState('');
  const [sort, setSort] = useState('id,desc');

  // Debounce each field individually
  const debouncedStatus = useDebounce(statusFilter, 300);
  const debouncedPatientName = useDebounce(patientNameFilter, 300);
  const debouncedPharmacistName = useDebounce(pharmacistNameFilter, 300);
  const debouncedId = useDebounce(idFilter, 300);
  const debouncedTableSearch = useDebounce(tableSearch, 300);
  const debouncedSort = useDebounce(sort, 300);

  const [billError, setBillError] = useState('');
  const [billDetails, setBillDetails] = useState(null);
  const { role } = useAuth();
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  const { bumpDashboard } = useDashboard();

  const load = async (p = page) => {
    const res = await api.get('/prescriptions', {
      params: {
        page: p,
        size,
        id: debouncedId || undefined,
        status: debouncedStatus || undefined,
        patientName: debouncedPatientName || undefined,
        pharmacistName: debouncedPharmacistName || undefined,
        q: debouncedTableSearch.trim() || undefined,
        sort: sort || undefined,
      },
    });
    setItems(res.data.content || []);
    setTotalPages(res.data.totalPages || 0);
    setPage(res.data.number || 0);
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  // live search (debounced) - triggers when any debounced value changes
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedStatus, debouncedId, debouncedPatientName, debouncedPharmacistName, debouncedTableSearch, debouncedSort]);

  const openBill = async (prescriptionId) => {
    setBillError('');
    try {
      const res = await api.get(`/bills/by-prescription/${prescriptionId}`);
      setBillDetails(res.data);
    } catch (e) {
      const data = e.response?.data;
      const msg =
        e.userMessage ||
        data?.message ||
        data?.error ||
        data ||
        'Bill not found for this prescription';
      setBillError(typeof msg === 'string' ? msg : 'Bill not found for this prescription');
    }
  };

  const clearFilters = () => {
    setStatusFilter('');
    setIdFilter('');
    setPatientNameFilter('');
    setPharmacistNameFilter('');
    setTableSearch('');
    setSort('id,desc');
    // clearFilters function wasn't explicitly defined before but logic might be needed if button added
  };

  const [deleteTarget, setDeleteTarget] = useState(null);

  const requestRemove = (prescription) => {
    setDeleteTarget(prescription);
  };

  const performRemove = async () => {
    if (!deleteTarget) return;
    await api.delete(`/prescriptions/${deleteTarget.id}`);
    setDeleteTarget(null);
    await load(page);
    bumpDashboard();
  };

  const statusClass = (status) => {
    if (status === 'PENDING') return 'status-pill--warn';
    if (status === 'FILLED') return 'status-pill--ok';
    return 'status-pill--danger';
  };

  const formatWalkInLabel = (patientName, walkInName, walkInPhone, fallback) => {
    const name = walkInName ? walkInName.trim() : '';
    const phone = walkInPhone ? walkInPhone.trim() : '';
    if (name || phone) {
      return `Walk-in: ${name || phone}`;
    }
    if (patientName && patientName.toLowerCase() === 'walk-in customer') {
      return 'Walk-in Customer';
    }
    return patientName || fallback || '-';
  };

  return (
    <div>
      <div className="section-title">Prescriptions</div>
      {billError && (
        <div className="error" style={{ marginBottom: '0.5rem' }}>
          {billError}
        </div>
      )}
      <div className="card-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Filter by ID"
              type="number"
              min="1"
              value={idFilter}
              onChange={(e) => setIdFilter(e.target.value)}
              style={{ width: '100px' }}
            />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">Any Status</option>
              <option value="PENDING">PENDING</option>
              <option value="FILLED">FILLED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
            {role !== 'PATIENT' && (
              <input
                placeholder="Patient name"
                value={patientNameFilter}
                onChange={(e) => setPatientNameFilter(e.target.value)}
              />
            )}
            {role === 'ADMIN' && (
              <input
                placeholder="Pharmacist name"
                value={pharmacistNameFilter}
                onChange={(e) => setPharmacistNameFilter(e.target.value)}
              />
            )}
            <PrimaryButton type="button" variant="secondary" onClick={clearFilters}>
              Clear
            </PrimaryButton>
          </form>
          <div className="filters" style={{ marginTop: '0.25rem' }}>
            <input
              placeholder="Search by ID, patient, pharmacist, status, bill..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
              style={{ flex: 1 }}
            />
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="id,desc">ID: High → Low</option>
              <option value="id,asc">ID: Low → High</option>
              <option value="dateIssued,desc">Date: Newest → Oldest</option>
              <option value="dateIssued,asc">Date: Oldest → Newest</option>
            </select>
          </div>
          {role !== 'PATIENT' && (
            <div style={{ marginTop: '0.5rem' }}>
              <PrimaryButton type="button" onClick={() => navigate('/prescriptions/new')}>
                New prescription
              </PrimaryButton>
            </div>
          )}
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Patient</th>
            <th>Pharmacist</th>
            <th>Status</th>
            <th>Date</th>
            <th>Bill</th>
            <th style={{ width: 260 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className={focusId === p.id ? 'focus-row' : ''}>
              <td>{p.id}</td>
              <td>{formatWalkInLabel(p.patientName, p.walkInName, p.walkInPhone, p.personId)}</td>
              <td>{p.pharmacistName || p.pharmacistId}</td>
              <td>
                <span className={`status-pill ${statusClass(p.status)}`}>
                  {p.status}
                </span>
              </td>
              <td>{p.dateIssued}</td>
              <td>
                {p.status === 'FILLED' ? (
                  <PrimaryButton
                    type="button"
                    variant="secondary"
                    onClick={() => openBill(p.id)}
                  >
                    View bill
                  </PrimaryButton>
                ) : (
                  <span className="chip">Not billed</span>
                )}
              </td>
              <td>
                <PrimaryButton
                  type="button"
                  variant="secondary"
                  onClick={() => navigate(`/prescriptions/${p.id}`)}
                >
                  View
                </PrimaryButton>{' '}
                {role !== 'PATIENT' && (
                  <>
                    {p.status !== 'CANCELLED' && (
                      <>
                        <PrimaryButton
                          type="button"
                          onClick={() => navigate(`/prescriptions/${p.id}/edit`)}
                        >
                          Edit
                        </PrimaryButton>{' '}
                      </>
                    )}
                    {(role === 'ADMIN' || role === 'PHARMACIST') && p.status !== 'FILLED' && p.status !== 'CANCELLED' && (
                      <PrimaryButton
                        type="button"
                        variant="danger"
                        onClick={() => requestRemove(p)}
                      >
                        Delete
                      </PrimaryButton>
                    )}
                  </>
                )}
              </td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={7}>No prescriptions found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination
        page={page}
        totalPages={totalPages}
        onPageChange={load}
      />

      {deleteTarget && (
        <Modal
          title="Delete prescription"
          onClose={() => setDeleteTarget(null)}
          primaryLabel="Delete"
          onPrimary={performRemove}
          secondaryLabel="Cancel"
        >
          <p>
            Are you sure you want to soft delete prescription #{deleteTarget.id}?
          </p>
        </Modal>
      )}

      {billDetails && (
        <Modal
          title={`Bill #${billDetails.id} (Rx #${billDetails.prescriptionId})`}
          onClose={() => setBillDetails(null)}
          secondaryLabel="Close"
        >
          <div className="form-grid">
            <div className="field">
              <label htmlFor="bill-status">Status</label>
              <input id="bill-status" value={billDetails.status} readOnly
                style={{
                  color: billDetails.status === 'PAID' ? 'var(--ph-primary)' : 'var(--ph-warning)',
                  fontWeight: 'bold'
                }}
              />
            </div>
            <div className="field">
              <label htmlFor="bill-payment-method">Payment method</label>
              <input id="bill-payment-method" value={
                billDetails.paymentMethod === 'CASH' ? '💵 Cash' :
                  billDetails.paymentMethod === 'CARD' ? '💳 Card' :
                    billDetails.paymentMethod === 'MOBILE' ? '📱 Mobile Money' :
                      billDetails.paymentMethod || "N/A"
              } readOnly />
            </div>
            <div className="field">
              <label htmlFor="bill-payment-ref">Payment reference</label>
              <input id="bill-payment-ref" value={billDetails.paymentRef || "—"} readOnly />
            </div>
            <div className="field">
              <label htmlFor="bill-subtotal">Subtotal</label>
              <input id="bill-subtotal" value={`${billDetails.subtotal} ${billDetails.currency || 'RWF'}`} readOnly />
            </div>
            <div className="field">
              <label htmlFor="bill-net-total">Total Amount</label>
              <input id="bill-net-total" value={`${billDetails.netTotal} ${billDetails.currency || 'RWF'}`} readOnly
                style={{ fontWeight: 'bold', fontSize: '1.1rem' }}
              />
            </div>
            {billDetails.status === 'PAID' && billDetails.paymentDate && (
              <div className="field">
                <label htmlFor="bill-payment-date">Payment date</label>
                <input id="bill-payment-date" value={billDetails.paymentDate.split('T')[0]} readOnly />
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}


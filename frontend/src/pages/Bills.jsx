import { useEffect, useState } from 'react';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';
import { useAuth } from '../context/AuthContext';
import { useSearchParams } from 'react-router-dom';

export default function Bills() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState({
    status: '',
    paymentMethod: '',
    paymentRef: '',
    prescriptionId: '',
    patientName: '',
    id: '',
  });
  const [sort, setSort] = useState('id,desc');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);

  const [form, setForm] = useState({
    status: 'PENDING',
    paymentMethod: '',
    paymentRef: '',
    discountAmount: 0,
    taxAmount: 0,
    paymentDate: '',
  });
  const [deleteTarget, setDeleteTarget] = useState(null);
  // debounce filters to avoid rapid requests
  const debouncedFilter = useDebounce(filter, 300);
  const debouncedSort = useDebounce(sort, 300);
  const { role } = useAuth();
  const [tableSearch, setTableSearch] = useState('');
  const debouncedTableSearch = useDebounce(tableSearch, 300);
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  // Local toast-style feedback so we avoid undefined context methods
  const [toast, setToast] = useState(null);
  const showToast = (type, message) => setToast({ type, message });
  const clearToast = () => setToast(null);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  const openEdit = async (id) => {
    try {
      const res = await api.get(`/bills/${id}`);
      setEditing(id);
      const pd = res.data.paymentDate ? String(res.data.paymentDate).slice(0, 16) : '';
      setForm({ ...res.data, paymentDate: pd });
      setIsModalOpen(true);
    } catch (err) {
      showToast('error', err.message || 'Failed to load bill');
    }
  };

  const save = async () => {
    try {
      if (!editing) {
        showToast('error', 'Bill creation is disabled. Bills are created when dispensing.');
        return;
      }
      // Normalize payload to what backend expects (numbers + seconds in datetime)
      const paymentDate =
        form.paymentDate && form.paymentDate.length === 16
          ? `${form.paymentDate}:00`
          : form.paymentDate;

      const data = {
        status: form.status,
        paymentMethod: form.paymentMethod || null,
        paymentRef: form.paymentRef || null,
        discountAmount:
          form.discountAmount === '' || form.discountAmount === null
            ? null
            : Number(form.discountAmount),
        taxAmount:
          form.taxAmount === '' || form.taxAmount === null
            ? null
            : Number(form.taxAmount),
        paymentDate,
      };

      await api.put(`/bills/${editing}`, data);
      showToast('info', 'Bill updated');
      setIsModalOpen(false);
      load();
    } catch (err) {
      showToast('error', err.message || 'Failed to save bill');
    }
  };

  const requestRemove = (item) => {
    setDeleteTarget(item);
  };

  const performRemove = async () => {
    try {
      await api.delete(`/bills/${deleteTarget.id}`);
      showToast('info', `Bill #${deleteTarget.id} deleted`);
      setDeleteTarget(null);
      load();
    } catch (err) {
      showToast('error', err.message || 'Failed to delete bill');
    }
  };

  const load = async (p = page) => {
    setPage(p);
    try {
      const qTerm = debouncedTableSearch.trim();
      const res = await api.get('/bills', {
        params: {
          page: p,
          size,
          id: filter.id || undefined,
          status: filter.status || undefined,
          paymentMethod: filter.paymentMethod || undefined,
          paymentRef: filter.paymentRef || undefined,
          prescriptionId: filter.prescriptionId || undefined,
          patientName: filter.patientName || undefined,
          q: qTerm || undefined,
          sort: sort || undefined,
        },
      });
      setItems(res.data?.content || []);
      setTotalPages(res.data?.totalPages ?? 0);
      setPage(res.data?.number ?? p);
    } catch (err) {
      const msg =
        err.userMessage ||
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message ||
        'Failed to load bills';
      showToast('error', msg);
    }
  };

  const clearFilters = () => {
    setFilter({
      status: '',
      paymentMethod: '',
      paymentRef: '',
      prescriptionId: '',
      patientName: '',
      id: '',
    });
    setSort('id,desc');
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // live search on status / payment method / ref (debounced)
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter, debouncedSort, debouncedTableSearch]);

  const formatWalkInLabel = (customerName, customerPhone, patientName) => {
    const name = customerName ? customerName.trim() : '';
    const phone = customerPhone ? customerPhone.trim() : '';
    if (name || phone) {
      return `Walk-in: ${name || phone}`;
    }
    if (patientName && patientName.toLowerCase() === 'walk-in customer') {
      return 'Walk-in Customer';
    }
    return patientName || '-';
  };

  return (
    <div>
      {toast && (
        <div
          className={toast.type === 'error' ? 'error' : 'info'}
          style={{ marginBottom: '0.5rem' }}
          role="status"
        >
          {toast.message}
          <PrimaryButton
            type="button"
            variant="ghost"
            style={{ marginLeft: '0.5rem' }}
            onClick={clearToast}
          >
            Close
          </PrimaryButton>
        </div>
      )}
      <div className="page-header">
        <h1>Bills</h1>
        <div className="page-header__actions">
          <form onSubmit={onSearch} className="filters">
            <>
              <input
                type="number"
                placeholder="Bill ID"
                value={filter.id}
                onChange={(e) =>
                  setFilter({ ...filter, id: e.target.value })
                }
                style={{ width: '80px' }}
              />
              <input
                type="number"
                placeholder="Prescription ID"
                value={filter.prescriptionId}
                onChange={(e) =>
                  setFilter({ ...filter, prescriptionId: e.target.value })
                }
                style={{ width: '110px' }}
              />
              {role !== 'PATIENT' && (
                <input
                  placeholder="Patient name"
                  value={filter.patientName}
                  onChange={(e) =>
                    setFilter({ ...filter, patientName: e.target.value })
                  }
                />
              )}
            </>
            <select
              value={filter.status}
              onChange={(e) => setFilter({ ...filter, status: e.target.value })}
            >
              <option value="">Any Status</option>
              <option value="PENDING">PENDING</option>
              <option value="PAID">PAID</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
            <select
              value={filter.paymentMethod}
              onChange={(e) =>
                setFilter({ ...filter, paymentMethod: e.target.value })
              }
            >
              <option value="">Any Method</option>
              <option value="CASH">CASH</option>
              <option value="CARD">CARD</option>
              <option value="MOBILE">MOBILE</option>
            </select>
            <input
              placeholder="Payment ref"
              value={filter.paymentRef}
              onChange={(e) =>
                setFilter({ ...filter, paymentRef: e.target.value })
              }
            />
            <PrimaryButton
              type="button"
              variant="secondary"
              onClick={clearFilters}
            >
              Clear filters
            </PrimaryButton>
          </form>
          <div className="filters" style={{ marginTop: '0.25rem' }}>
            <input
              placeholder="Search by ID, rx, patient, status, method, ref, amount..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
              style={{ flex: 1 }}
            />
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="id,desc">ID: High → Low</option>
              <option value="id,asc">ID: Low → High</option>
              <option value="netTotal,desc">Amount: High → Low</option>
              <option value="netTotal,asc">Amount: Low → High</option>
            </select>
          </div>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Rx</th>
            <th>Patient</th>
            <th>Status</th>
            <th>Method</th>
            <th>Ref</th>
            <th>Subtotal</th>
            <th>Net total</th>
            <th>Date</th>
            <th style={{ width: 170 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((b) => (
            <tr key={b.id} className={focusId === b.id ? 'focus-row' : ''}>
              <td>{b.id}</td>
              <td>{b.prescriptionId}</td>
              <td>{formatWalkInLabel(b.customerName, b.customerPhone, b.patientName)}</td>
              <td>
                <span
                  className={`status-pill ${b.status === 'PAID'
                    ? 'status-pill--ok'
                    : b.status === 'PENDING'
                      ? 'status-pill--warn'
                      : 'status-pill--danger'
                    }`}
                >
                  {b.status}
                </span>
              </td>
              <td>{b.paymentMethod}</td>
              <td>{b.paymentRef}</td>
              <td>{b.subtotal}</td>
              <td>{b.netTotal}</td>
              <td>{b.paymentDate ? String(b.paymentDate).slice(0, 16) : ''}</td>
              <td>
                {role !== 'PATIENT' ? (
                  <>
                    {b.status === 'PAID' ? (
                      <PrimaryButton
                        type="button"
                        variant="secondary"
                        onClick={() => window.location.href = `/bills/${b.id}/receipt`}
                      >
                        Print Receipt
                      </PrimaryButton>
                    ) : (
                      <>
                        <PrimaryButton
                          type="button"
                          variant="secondary"
                          onClick={() => openEdit(b.id)}
                        >
                          Edit
                        </PrimaryButton>{' '}
                        <PrimaryButton
                          type="button"
                          variant="danger"
                          onClick={() => requestRemove(b)}
                        >
                          Delete
                        </PrimaryButton>
                      </>
                    )}
                  </>
                ) : (
                  /* Patient view - can print PAID bills */
                  b.status === 'PAID' ? (
                    <PrimaryButton
                      type="button"
                      variant="secondary"
                      onClick={() => window.location.href = `/bills/${b.id}/receipt`}
                    >
                      🧾 Print
                    </PrimaryButton>
                  ) : (
                    <span className="chip">Pending</span>
                  )
                )}
              </td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={10}>No bills found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isModalOpen && (
        <Modal
          title="Edit bill"
          onClose={() => setIsModalOpen(false)}
          primaryLabel="Save"
          onPrimary={save}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            <div className="field">
              <label>Status</label>
              <select
                value={form.status}
                onChange={(e) => {
                  const nextStatus = e.target.value;
                  setForm({
                    ...form,
                    status: nextStatus,
                    paymentMethod: nextStatus === 'PENDING' ? '' : form.paymentMethod,
                  });
                }}
              >
                <option value="PENDING">PENDING</option>
                <option value="PAID">PAID</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>
            <div className="field">
              <label>Payment method</label>
              <select
                value={form.paymentMethod}
                disabled={form.status === 'PENDING'}
                onChange={(e) =>
                  setForm({ ...form, paymentMethod: e.target.value })
                }
              >
                <option value="">None</option>
                <option value="CASH">CASH</option>
                <option value="CARD">CARD</option>
                <option value="MOBILE">MOBILE</option>
              </select>
            </div>
            <div className="field">
              <label>Payment reference</label>
              <input
                value={form.paymentRef}
                onChange={(e) =>
                  setForm({ ...form, paymentRef: e.target.value })
                }
              />
              {form.status === 'PAID' && (
                <small style={{ color: 'var(--ph-danger)' }}>
                  Payment reference is required for PAID bills.
                </small>
              )}
            </div>
            <div className="field">
              <label>Discount amount</label>
              <input
                type="number"
                step="0.01"
                value={form.discountAmount}
                onChange={(e) =>
                  setForm({ ...form, discountAmount: e.target.value })
                }
              />
            </div>
            <div className="field">
              <label>Tax amount</label>
              <input
                type="number"
                step="0.01"
                value={form.taxAmount}
                onChange={(e) => setForm({ ...form, taxAmount: e.target.value })}
              />
            </div>
            {/* Payment date is auto-set to NOW when marking as PAID - not editable */}
          </div>
        </Modal>
      )}

      {deleteTarget && (
        <Modal
          title="Delete bill"
          onClose={() => setDeleteTarget(null)}
          primaryLabel="Delete"
          onPrimary={performRemove}
          secondaryLabel="Cancel"
        >
          <p>
            Are you sure you want to delete bill #{deleteTarget.id}?</p>
        </Modal>
      )}
    </div>
  );
}



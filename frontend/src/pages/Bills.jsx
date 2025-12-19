import { useEffect, useState } from 'react';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';

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
  });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    prescriptionId: '',
    status: 'PENDING',
    paymentMethod: 'CASH',
    paymentRef: '',
    discountAmount: 0,
    taxAmount: 0,
    paymentDate: '',
  });
  const [deleteTarget, setDeleteTarget] = useState(null);
  // debounce filters to avoid rapid requests
  const debouncedFilter = useDebounce(filter, 300);

  // Local toast-style feedback so we avoid undefined context methods
  const [toast, setToast] = useState(null);
  const showToast = (type, message) => setToast({ type, message });
  const clearToast = () => setToast(null);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  const openCreate = () => {
    setEditing(null);
    setForm({
      prescriptionId: '',
      status: 'PENDING',
      paymentMethod: 'CASH',
      paymentRef: '',
      discountAmount: 0,
      taxAmount: 0,
      paymentDate: new Date().toISOString().slice(0, 16),
    });
    setIsModalOpen(true);
  };

  const openEdit = async (id) => {
    try {
      const res = await api.get(`/bills/${id}`);
      setEditing(id);
      setForm({ ...res.data, paymentDate: new Date(res.data.paymentDate).toISOString().slice(0, 16) });
      setIsModalOpen(true);
    } catch (err) {
      showToast('error', err.message || 'Failed to load bill');
    }
  };

  const save = async () => {
    try {
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

      if (editing) {
        await api.put(`/bills/${editing}`, data);
        showToast('info', 'Bill updated');
      } else {
        await api.post(`/bills/for-prescription/${form.prescriptionId}`, data);
        showToast('info', 'Bill created');
      }
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
    try {
      const res = await api.get('/bills', {
        params: {
          page: p,
          size,
          status: filter.status || undefined,
          paymentMethod: filter.paymentMethod || undefined,
          paymentRef: filter.paymentRef || undefined,
          prescriptionId: filter.prescriptionId || undefined,
          patientName: filter.patientName || undefined,
        },
      });
      setItems(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      showToast('error', err.message || 'Failed to load bills');
    }
  };

  const clearFilters = () => {
    setFilter({ status: '', paymentMethod: '', paymentRef: '', prescriptionId: '', patientName: '' });
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // live search on status / payment method / ref (debounced)
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter]);

  return (
    <div>
      {toast && (
        <div
          className={toast.type === 'error' ? 'error' : 'info'}
          style={{ marginBottom: '0.5rem' }}
          role="status"
        >
          {toast.message}
          <button
            type="button"
            className="btn btn-ghost"
            style={{ marginLeft: '0.5rem' }}
            onClick={clearToast}
          >
            Close
          </button>
        </div>
      )}
      <div className="page-header">
        <h1>Bills</h1>
        <div className="page-header__actions">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Patient name"
              value={filter.patientName}
              onChange={(e) =>
                setFilter({ ...filter, patientName: e.target.value })
              }
            />
            <input
              type="number"
              placeholder="Prescription ID"
              value={filter.prescriptionId}
              onChange={(e) =>
                setFilter({ ...filter, prescriptionId: e.target.value })
              }
            />
            <select
              value={filter.status}
              onChange={(e) => setFilter({ ...filter, status: e.target.value })}
            >
              <option value="">Any Status</option>
              <option value="PENDING">PENDING</option>
              <option value="PAID">PAID</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
            <input
              placeholder="Payment ref"
              value={filter.paymentRef}
              onChange={(e) =>
                setFilter({ ...filter, paymentRef: e.target.value })
              }
            />
            <button
              type="button"
              className="btn btn-secondary"
              onClick={clearFilters}
            >
              Clear filters
            </button>
          </form>
          <div style={{ marginTop: '0.5rem' }}>
            <PrimaryButton type="button" onClick={() => openCreate()}>
              New bill for prescription
            </PrimaryButton>
          </div>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Rx</th>
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
            <tr key={b.id}>
              <td>{b.id}</td>
              <td>{b.prescriptionId}</td>
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
              <td>{b.paymentDate}</td>
              <td>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => openEdit(b.id)}
                >
                  Edit
                </button>{' '}
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => requestRemove(b)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={9}>No bills found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isModalOpen && (
        <Modal
          title={editing ? 'Edit bill' : 'New bill for prescription'}
          onClose={() => setIsModalOpen(false)}
          primaryLabel="Save"
          onPrimary={save}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            {!editing && (
              <div className="field">
                <label>Prescription ID</label>
                <input
                  value={form.prescriptionId}
                  onChange={(e) =>
                    setForm({ ...form, prescriptionId: e.target.value })
                  }
                />
              </div>
            )}
            <div className="field">
              <label>Status</label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
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
            <div className="field">
              <label>Payment date</label>
              <input
                type="datetime-local"
                value={form.paymentDate}
                onChange={(e) =>
                  setForm({ ...form, paymentDate: e.target.value })
                }
              />
            </div>
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

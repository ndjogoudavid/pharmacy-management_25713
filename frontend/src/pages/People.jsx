import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import LocationPicker from '../components/LocationPicker';
import { useDashboard } from '../context/DashboardContext';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';

export default function People() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState({ id: '', name: '', email: '', role: '', phone: '', location: '' });
  const [sort, setSort] = useState('name,asc');
  const [locationCache, setLocationCache] = useState({});
  const [error, setError] = useState('');
  const debouncedFilter = useDebounce(filter, 300);
  const debouncedSort = useDebounce(sort, 300);
  const [tableSearch, setTableSearch] = useState('');
  const debouncedTableSearch = useDebounce(tableSearch, 300);
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    role: 'PATIENT',
    locationId: '',
    password: '',
  });
  const { bumpDashboard } = useDashboard();

  const load = async (p = page) => {
    const qTerm = debouncedTableSearch.trim();
    const res = await api.get('/persons', {
      params: {
        page: p,
        size,
        id: filter.id || undefined,
        name: filter.name,
        email: filter.email,
        phone: filter.phone,
        location: filter.location,
        role: filter.role || undefined,
        q: qTerm || undefined,
        sort: debouncedSort || undefined,
      },
    });
    const people = res.data.content || [];
    setItems(people);
    setTotalPages(res.data.totalPages || 0);
    setPage(res.data.number || 0);
    await ensureLocationNames(people);
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  const clearFilters = () => {
    setFilter({ id: '', name: '', email: '', role: '', phone: '', location: '' });
    setSort('name,asc');
  };

  // live search: name/email/role (debounced)
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter, debouncedSort, debouncedTableSearch]);

  const ensureLocationNames = async (people) => {
    const missing = Array.from(
      new Set(
        people
          .map((p) => p.locationId)
          .filter((id) => id && !locationCache[id])
      )
    );
    if (missing.length === 0) return;
    const results = await Promise.all(
      missing.map((id) =>
        api
          .get(`/locations/${id}`)
          .then((res) => ({ id, data: res.data }))
          .catch(() => null)
      )
    );
    const next = { ...locationCache };
    results.forEach((r) => {
      if (r && r.data) {
        next[r.id] = r.data;
      }
    });
    setLocationCache(next);
  };

  const openCreate = () => {
    setEditing(null);
    setForm({
      name: '',
      email: '',
      phone: '',
      role: 'PATIENT',
      locationId: '',
      password: '',
    });
    setIsModalOpen(true);
  };

  const openEdit = async (id) => {
    const res = await api.get(`/persons/${id}`);
    const p = res.data;
    setEditing(p);
    setForm({
      name: p.name || '',
      email: p.email || '',
      phone: p.phone || '',
      role: p.role || 'PATIENT',
      locationId: p.locationId || '',
      password: '',
    });
    setIsModalOpen(true);
  };

  const save = async () => {
    setError('');

    if (!form.locationId) {
      setError(
        "Please select a location (at least province, ideally down to the patient's village)."
      );
      return;
    }

    const payload = {
      name: form.name,
      email: form.email,
      phone: form.phone,
      role: form.role,
      locationId: form.locationId,
      password: form.password || undefined,
    };

    try {
      if (editing && editing.id) {
        await api.put(`/persons/${editing.id}`, payload);
      } else {
        await api.post('/persons', payload);
      }
      setIsModalOpen(false);
      await load(page);
      bumpDashboard();
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
        'Failed to save person';
      setError(typeof msg === 'string' ? msg : 'Failed to save person');
    }
  };

  const [deleteTarget, setDeleteTarget] = useState(null);

  const confirmRemove = (person) => {
    setDeleteTarget(person);
  };

  const performRemove = async () => {
    if (!deleteTarget) return;
    await api.delete(`/persons/${deleteTarget.id}`);
    setDeleteTarget(null);
    await load(page);
    bumpDashboard();
  };

  return (
    <div>
      <div className="section-title">People</div>
      <div className="card-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Filter by ID"
              type="number"
              value={filter.id}
              onChange={(e) => setFilter({ ...filter, id: e.target.value })}
              style={{ maxWidth: '100px' }}
            />
            <input
              placeholder="Filter by name"
              value={filter.name}
              onChange={(e) => setFilter({ ...filter, name: e.target.value, email: '' })}
            />
            <input
              placeholder="Filter by email"
              value={filter.email}
              onChange={(e) => setFilter({ ...filter, email: e.target.value })}
            />
            <input
              placeholder="Filter by phone"
              value={filter.phone}
              onChange={(e) => setFilter({ ...filter, phone: e.target.value })}
            />
            <input
              placeholder="Filter by location"
              value={filter.location}
              onChange={(e) => setFilter({ ...filter, location: e.target.value })}
            />
            <select
              value={filter.role}
              onChange={(e) => setFilter({ ...filter, role: e.target.value })}
            >
              <option value="">Any Role</option>
              <option value="ADMIN">ADMIN</option>
              <option value="PHARMACIST">PHARMACIST</option>
              <option value="PATIENT">PATIENT</option>
            </select>
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="name,asc">Name: A → Z</option>
              <option value="name,desc">Name: Z → A</option>
              <option value="id,asc">ID: Low → High</option>
              <option value="id,desc">ID: High → Low</option>
            </select>
            <PrimaryButton
              type="button"
              variant="secondary"
              onClick={clearFilters}
            >
              Clear filters
            </PrimaryButton>
          </form>
          <div className="filters" style={{ marginTop: '0.5rem' }}>
            <input
              placeholder="🔍 Search by name, email, phone, location, role..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
            />
            <PrimaryButton type="button" onClick={openCreate}>
              New person
            </PrimaryButton>
          </div>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Role</th>
            <th>Location</th>
            <th style={{ width: 180 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className={focusId === p.id ? 'focus-row' : ''}>
              <td>{p.id}</td>
              <td>{p.name}</td>
              <td>{p.email}</td>
              <td>{p.phone}</td>
              <td>
                <span className="chip">{p.role}</span>
              </td>
              <td>
                {p.locationId
                  ? locationCache[p.locationId]?.name || `Location #${p.locationId}`
                  : '-'}
              </td>
              <td>
                <PrimaryButton
                  type="button"
                  variant="secondary"
                  onClick={() => openEdit(p.id)}
                >
                  Edit
                </PrimaryButton>{' '}
                <PrimaryButton
                  type="button"
                  variant="danger"
                  onClick={() => confirmRemove(p)}
                >
                  Delete
                </PrimaryButton>
              </td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={7}>No people found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isModalOpen && (
        <Modal
          title={editing ? 'Edit person' : 'New person'}
          onClose={() => setIsModalOpen(false)}
          primaryLabel="Save"
          onPrimary={save}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            <div className="field">
              <label>Name</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g. Alice Uwimana"
              />
            </div>
            <div className="field">
              <label>Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="e.g. alice@example.com"
              />
            </div>
            <div className="field">
              <label>Phone</label>
              <input
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="+250788123456"
              />
            </div>
            <div className="field">
              <label>Role</label>
              <select
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                <option value="ADMIN">ADMIN</option>
                <option value="PHARMACIST">PHARMACIST</option>
                <option value="PATIENT">PATIENT</option>
              </select>
            </div>
            <div className="field">
              <label>Location</label>
              <LocationPicker
                value={form.locationId}
                onChange={(id) =>
                  setForm({ ...form, locationId: id || '' })
                }
              />
            </div>
            <div className="field">
              <label>{editing ? 'New password (optional)' : 'Password'}</label>
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder={editing ? 'Leave blank to keep current password' : 'Choose a secure password'}
              />
            </div>
          </div>
          {error && (
            <div className="error" style={{ marginTop: '0.5rem' }}>
              {error}
            </div>
          )}
        </Modal>
      )}

      {deleteTarget && (
        <Modal
          title="Delete person"
          onClose={() => setDeleteTarget(null)}
          primaryLabel="Delete"
          onPrimary={performRemove}
          secondaryLabel="Cancel"
        >
          <p>
            Are you sure you want to soft delete{' '}
            <strong>{deleteTarget.name}</strong>?
          </p>
        </Modal>
      )}
    </div>
  );
}

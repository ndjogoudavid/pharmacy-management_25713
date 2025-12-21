import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import { useDashboard } from '../context/DashboardContext';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';
import { useAuth } from '../context/AuthContext';

export default function Medicines() {
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState({ name: '', sku: '', manufacturer: '', type: '', unit: '', controlled: '' });
  const [sort, setSort] = useState('name,asc');
  const [error, setError] = useState('');

  const [categories, setCategories] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const categoryOptions = categories
    .map((c) => c.name)
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b));
  const debouncedFilter = useDebounce(filter, 300);
  const debouncedSort = useDebounce(sort, 300);
  const [tableSearch, setTableSearch] = useState('');
  const debouncedTableSearch = useDebounce(tableSearch, 300);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    sku: '',
    name: '',
    type: '',
    unit: '',
    quantity: 0,
    reorderLevel: 0,
    price: '',
    manufacturer: '',
    isControlled: false,
    expiryDate: '',
    categoryIds: [],
  });
  const { bumpDashboard } = useDashboard();
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  const load = async (p = page) => {
    setError('');
    const qTerm = debouncedTableSearch.trim();
    try {
      const res = await api.get('/medicines', {
        params: {
          page: p,
          size,
          name: filter.name || undefined,
          sku: filter.sku || undefined,
          manufacturer: filter.manufacturer || undefined,
          type: filter.type || undefined,
          unit: filter.unit || undefined,
          controlled: filter.controlled || undefined,
          q: qTerm || undefined,
          sort: sort || undefined,
        },
      });
      setItems(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setPage(res.data.number || 0);
    } catch (e) {
      const msg =
        e.userMessage ||
        e.response?.data?.message ||
        e.response?.data?.error ||
        'Failed to load medicines';
      setError(typeof msg === 'string' ? msg : 'Failed to load medicines');
      setItems([]);
      setTotalPages(0);
    }
  };

  const loadLowStock = async () => {
    const res = await api.get('/medicines/low-stock');
    setLowStock(res.data || []);
  };

  const loadCategories = async () => {
    const res = await api.get('/categories', { params: { page: 0, size: 100 } });
    setCategories(res.data.content || []);
  };

  useEffect(() => {
    load(0);
    loadLowStock();
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  const clearFilters = () => {
    setFilter({ name: '', sku: '', manufacturer: '', type: '', unit: '', controlled: '' });
    setSort('name,asc');
    setTableSearch('');
  };

  // live search whenever name/SKU change (debounced)
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter, debouncedSort, debouncedTableSearch]);

  const openCreate = () => {
    if (!isAdmin) return;
    setEditing(null);
    setForm({
      sku: '',
      name: '',
      type: '',
      unit: '',
      quantity: 0,
      reorderLevel: 0,
      price: '',
      manufacturer: '',
      isControlled: false,
      expiryDate: '',
      categoryIds: [],
    });
    setIsModalOpen(true);
  };

  const openEdit = async (id) => {
    if (!isAdmin) return;
    const res = await api.get(`/medicines/${id}`);
    const m = res.data;
    setEditing(m);
    setForm({
      sku: m.sku || '',
      name: m.name || '',
      type: m.type || m.categories?.[0]?.name || '',
      unit: m.unit || '',
      quantity: m.quantity ?? 0,
      reorderLevel: m.reorderLevel ?? 0,
      price: m.price ?? '',
      manufacturer: m.manufacturer || '',
      isControlled: m.isControlled ?? false,
      expiryDate: m.expiryDate || '',
      categoryIds: m.categoryIds || [],
    });
    setIsModalOpen(true);
  };

  const save = async () => {
    if (!isAdmin) return;
    const payload = {
      ...form,
      type:
        form.type ||
        categories.find((c) => c.id === form.categoryIds?.[0])?.name ||
        '',
      quantity: Number(form.quantity ?? 0),
      reorderLevel: Number(form.reorderLevel ?? 0),
      price: form.price === '' ? 0 : Number(form.price),
    };
    if (editing && editing.id) {
      await api.put(`/medicines/${editing.id}`, payload);
    } else {
      await api.post('/medicines', payload);
    }
    setIsModalOpen(false);
    await load(page);
    await loadLowStock();
    bumpDashboard();
  };

  const [deleteTarget, setDeleteTarget] = useState(null);

  const requestRemove = (medicine) => {
    if (!isAdmin) return;
    setDeleteTarget(medicine);
  };

  const performRemove = async () => {
    if (!isAdmin) return;
    if (!deleteTarget) return;
    await api.delete(`/medicines/${deleteTarget.id}`);
    setDeleteTarget(null);
    await load(page);
    await loadLowStock();
    bumpDashboard();
  };

  const handleCategoryChange = (e) => {
    const values = Array.from(e.target.selectedOptions).map((o) => Number(o.value));
    setForm({ ...form, categoryIds: values });
  };

  return (
    <div>
      <div className="section-title">Medicines</div>
      {error && (
        <div className="error" style={{ marginBottom: '0.75rem' }}>
          {error}
        </div>
      )}
      <div className="card-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Filter by name"
              value={filter.name}
              onChange={(e) => setFilter({ ...filter, name: e.target.value })}
            />
            <input
              placeholder="Filter by SKU"
              value={filter.sku}
              onChange={(e) => setFilter({ ...filter, sku: e.target.value })}
            />
            <input
              placeholder="Filter by manufacturer"
              value={filter.manufacturer}
              onChange={(e) => setFilter({ ...filter, manufacturer: e.target.value })}
            />
            <select
              value={filter.type}
              onChange={(e) => setFilter({ ...filter, type: e.target.value })}
            >
              <option value="">Any Category</option>
              {categoryOptions.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
            <select
              value={filter.unit}
              onChange={(e) => setFilter({ ...filter, unit: e.target.value })}
            >
              <option value="">Any Unit</option>
              <option value="tablet">tablet</option>
              <option value="capsule">capsule</option>
              <option value="tube">tube</option>
              <option value="bottle">bottle</option>
              <option value="vial">vial</option>
              <option value="ml">ml</option>
              <option value="mg">mg</option>
            </select>
            <select
              value={filter.controlled}
              onChange={(e) => setFilter({ ...filter, controlled: e.target.value })}
            >
              <option value="">Controlled?</option>
              <option value="true">Controlled</option>
              <option value="false">Regular</option>
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
              placeholder="🔍 Search by name, SKU, type, unit, controlled..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
            />
            {isAdmin && (
              <PrimaryButton type="button" onClick={openCreate}>
                New medicine
              </PrimaryButton>
            )}
          </div>
        </div>
        <div className="card">
          <div className="section-title">Low stock</div>
          {lowStock.length === 0 && <div style={{ fontSize: '0.85rem' }}>All stocks look healthy.</div>}
          {lowStock.map((m) => (
            <div key={m.id} style={{ fontSize: '0.85rem', marginBottom: '0.35rem' }}>
              <span>{m.name}</span>{' '}
              <span className="status-pill status-pill--warn">
                {m.quantity} units
              </span>
            </div>
          ))}
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>SKU</th>
            <th>Category</th>
            <th>Unit</th>
            <th>Qty</th>
            <th>Reorder</th>
            <th>Price</th>
            <th>Controlled</th>
            {isAdmin && <th style={{ width: 160 }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {items.map((m) => (
            <tr key={m.id} className={focusId === m.id ? 'focus-row' : ''}>
              <td>{m.id}</td>
              <td>{m.name}</td>
              <td>{m.sku}</td>
              <td>
                {m.categories?.length
                  ? m.categories.map((c) => c.name).join(', ')
                  : m.type}
              </td>
              <td>{m.unit}</td>
              <td>{m.quantity}</td>
              <td>{m.reorderLevel}</td>
              <td>{m.price}</td>
              <td>
                {m.isControlled ? (
                  <span className="status-pill status-pill--danger">Controlled</span>
                ) : (
                  <span className="status-pill status-pill--ok">Regular</span>
                )}
              </td>
              {isAdmin && (
                <td>
                  <PrimaryButton
                    type="button"
                    variant="secondary"
                    onClick={() => openEdit(m.id)}
                  >
                    Edit
                  </PrimaryButton>{' '}
                  <PrimaryButton
                    type="button"
                    variant="danger"
                    onClick={() => requestRemove(m)}
                  >
                    Delete
                  </PrimaryButton>
                </td>
              )}
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={isAdmin ? 10 : 9}>No medicines found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isAdmin && isModalOpen && (
        <Modal
          title={editing ? 'Edit medicine' : 'New medicine'}
          onClose={() => setIsModalOpen(false)}
          primaryLabel="Save"
          onPrimary={save}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            <div className="field">
              <label>SKU *</label>
              <input
                value={form.sku}
                onChange={(e) => setForm({ ...form, sku: e.target.value })}
                placeholder="e.g. SKU-0001"
              />
            </div>
            <div className="field">
              <label>Name *</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g. Paracetamol 500mg"
              />
            </div>
            <div className="field">
              <label>Category</label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
              >
                <option value="">Select category...</option>
                {categoryOptions.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Unit</label>
              <select
                value={form.unit}
                onChange={(e) => setForm({ ...form, unit: e.target.value })}
              >
                <option value="">Select unit...</option>
                <option value="tablet">Tablet</option>
                <option value="capsule">Capsule</option>
                <option value="bottle">Bottle</option>
                <option value="tube">Tube</option>
                <option value="vial">Vial</option>
                <option value="sachet">Sachet</option>
                <option value="injection">Injection</option>
                <option value="drop">Drop</option>
                <option value="cream">Cream</option>
                <option value="syrup">Syrup (ml)</option>
                <option value="pack">Pack</option>
                <option value="unit">Unit</option>
              </select>
            </div>
            <div className="field">
              <label>Current Stock</label>
              {editing ? (
                <div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <input
                      type="number"
                      value={form.quantity}
                      readOnly
                      disabled
                      style={{ flex: 1, background: 'var(--ph-bg-alt)', cursor: 'not-allowed' }}
                    />
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.85rem', color: 'var(--ph-text-muted)' }}>Add:</span>
                    <input
                      type="number"
                      min="0"
                      placeholder="0"
                      id="restock-amount"
                      style={{ width: '80px' }}
                    />
                    <PrimaryButton
                      type="button"
                      variant="secondary"
                      onClick={() => {
                        const input = document.getElementById('restock-amount');
                        const addQty = Number(input?.value || 0);
                        if (addQty > 0) {
                          setForm({ ...form, quantity: Number(form.quantity) + addQty });
                          input.value = '';
                        }
                      }}
                    >
                      +Restock
                    </PrimaryButton>
                  </div>
                </div>
              ) : (
                <input
                  type="number"
                  min="0"
                  value={form.quantity}
                  onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                  placeholder="Initial stock quantity"
                />
              )}
            </div>
            <div className="field">
              <label>Reorder Level</label>
              <input
                type="number"
                min="0"
                value={form.reorderLevel}
                onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })}
                placeholder="Alert when stock falls below this"
              />
            </div>
            <div className="field">
              <label>Price (RWF)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={form.price}
                onChange={(e) => setForm({ ...form, price: e.target.value })}
                placeholder="Unit price"
              />
            </div>
            <div className="field">
              <label>Manufacturer</label>
              <input
                value={form.manufacturer}
                onChange={(e) => setForm({ ...form, manufacturer: e.target.value })}
                placeholder="e.g. Pharmalab, GSK"
              />
            </div>
            <div className="field">
              <label>Expiry Date</label>
              <input
                type="date"
                value={form.expiryDate}
                onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
              />
              <small style={{ color: 'var(--ph-text-muted)' }}>When this batch expires</small>
            </div>
            <div className="field">
              <label>Controlled Substance</label>
              <select
                value={form.isControlled ? 'YES' : 'NO'}
                onChange={(e) =>
                  setForm({ ...form, isControlled: e.target.value === 'YES' })
                }
              >
                <option value="NO">No - Regular medicine</option>
                <option value="YES">Yes - Requires special handling</option>
              </select>
            </div>
            <div className="field" style={{ gridColumn: '1 / -1' }}>
              <label>Categories</label>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.25rem' }}>
                {categories.map((c) => (
                  <label
                    key={c.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                      padding: '0.4rem 0.75rem',
                      background: form.categoryIds.includes(c.id) ? 'var(--ph-primary)' : 'var(--ph-bg-alt)',
                      color: form.categoryIds.includes(c.id) ? 'white' : 'var(--ph-text)',
                      borderRadius: '20px',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      border: '1px solid var(--ph-border)',
                      transition: 'all 0.2s'
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={form.categoryIds.includes(c.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setForm({ ...form, categoryIds: [...form.categoryIds, c.id] });
                        } else {
                          setForm({ ...form, categoryIds: form.categoryIds.filter(id => id !== c.id) });
                        }
                      }}
                      style={{ display: 'none' }}
                    />
                    {c.name}
                  </label>
                ))}
              </div>
              <small style={{ color: 'var(--ph-text-muted)', marginTop: '0.5rem', display: 'block' }}>
                Click to toggle categories
              </small>
            </div>
          </div>
        </Modal>
      )}

      {isAdmin && deleteTarget && (
        <Modal
          title="Delete medicine"
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

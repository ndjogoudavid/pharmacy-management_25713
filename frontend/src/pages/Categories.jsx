import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';
import { useAuth } from '../context/AuthContext';

export default function Categories() {
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState({ id: '', name: '', description: '' });
  const [sort, setSort] = useState('name,asc');
  const debouncedFilter = useDebounce(filter, 300);
  const debouncedSort = useDebounce(sort, 300);
  const [tableSearch, setTableSearch] = useState('');
  const debouncedTableSearch = useDebounce(tableSearch, 300);
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '' });

  const load = async (p = page) => {
    setLoading(true);
    try {
      const qTerm = debouncedTableSearch.trim();
      const res = await api.get('/categories', {
        params: {
          page: p,
          size,
          id: filter.id || undefined,
          name: filter.name || undefined,
          description: filter.description || undefined,
          q: qTerm || undefined,
          sort: debouncedSort || undefined,
        },
      });
      setItems(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setPage(res.data.number || 0);
    } finally {
      setLoading(false);
    }
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
    setFilter({ id: '', name: '', description: '' });
    setSort('name,asc');
    setTableSearch('');
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter, debouncedSort, debouncedTableSearch]);

  const openCreate = () => {
    if (!isAdmin) return;
    setEditing(null);
    setForm({ name: '', description: '' });
    setIsModalOpen(true);
  };

  const openEdit = async (id) => {
    if (!isAdmin) return;
    const res = await api.get(`/categories/${id}`);
    setEditing(res.data);
    setForm({ name: res.data.name || '', description: res.data.description || '' });
    setIsModalOpen(true);
  };

  const save = async () => {
    if (!isAdmin) return;
    if (!form.name.trim()) {
      return;
    }
    if (editing) {
      await api.put(`/categories/${editing.id}`, form);
    } else {
      await api.post('/categories', form);
    }
    setIsModalOpen(false);
    await load(page);
  };

  const [deleteTarget, setDeleteTarget] = useState(null);

  const requestRemove = (category) => {
    if (!isAdmin) return;
    setDeleteTarget(category);
  };

  const performRemove = async () => {
    if (!isAdmin) return;
    if (!deleteTarget) return;
    await api.delete(`/categories/${deleteTarget.id}`);
    setDeleteTarget(null);
    await load(page);
  };

  return (
    <div>
      <div className="section-title">Medicine Categories</div>
      <div className="card-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Filter by ID"
              type="number"
              value={filter.id}
              onChange={(e) =>
                setFilter({ ...filter, id: e.target.value })
              }
              style={{ maxWidth: '100px' }}
            />
            <input
              placeholder="Filter by name"
              value={filter.name}
              onChange={(e) =>
                setFilter({ ...filter, name: e.target.value })
              }
            />
            <input
              placeholder="Filter by description"
              value={filter.description}
              onChange={(e) =>
                setFilter({ ...filter, description: e.target.value })
              }
            />
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
              placeholder="🔍 Search by name, description..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
            />
            <span style={{ fontSize: '0.8rem', color: 'var(--ph-text-muted)' }}>
              Organize medicines into reusable groups.
            </span>
            {isAdmin && <PrimaryButton onClick={openCreate}>New Category</PrimaryButton>}
          </div>
        </div>
      </div>
      {loading && <div>Loading...</div>}
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Description</th>
            {isAdmin && <th style={{ width: 150 }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {items.map((c) => (
            <tr key={c.id} className={focusId === c.id ? 'focus-row' : ''}>
              <td>{c.id}</td>
              <td>{c.name}</td>
              <td>{c.description}</td>
              {isAdmin && (
                <td>
                  <PrimaryButton
                    type="button"
                    variant="secondary"
                    onClick={() => openEdit(c.id)}
                  >
                    Edit
                  </PrimaryButton>{' '}
                  <PrimaryButton
                    type="button"
                    variant="danger"
                    onClick={() => requestRemove(c)}
                  >
                    Delete
                  </PrimaryButton>
                </td>
              )}
            </tr>
          ))}
          {items.length === 0 && !loading && (
            <tr>
              <td colSpan={isAdmin ? 4 : 3}>No categories yet.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isAdmin && isModalOpen && (
        <Modal
          title={editing ? 'Edit Category' : 'New Category'}
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
                placeholder="e.g. Antibiotics"
              />
            </div>
            <div className="field" style={{ gridColumn: '1 / -1' }}>
              <label>Description</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Optional details to help pharmacists"
              />
            </div>
          </div>
        </Modal>
      )}

      {isAdmin && deleteTarget && (
        <Modal
          title="Delete category"
          onClose={() => setDeleteTarget(null)}
          primaryLabel="Delete"
          onPrimary={performRemove}
          secondaryLabel="Cancel"
        >
          <p>
            Are you sure you want to delete category{' '}
            <strong>{deleteTarget.name}</strong>?
          </p>
        </Modal>
      )}
    </div>
  );
}

import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import PrimaryButton from '../components/PrimaryButton';
import Modal from '../components/Modal';
import { useDashboard } from '../context/DashboardContext';
import TablePagination from '../components/TablePagination';
import useDebounce from '../hooks/useDebounce';
import Typeahead from '../components/Typeahead';

export default function Locations() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState({ type: '', parentId: '', name: '', code: '' });
  const [sort, setSort] = useState('name,asc');
  const debouncedFilter = useDebounce(filter, 300);
  const debouncedSort = useDebounce(sort, 300);
  const [parentOptions, setParentOptions] = useState([]);
  const [tableSearch, setTableSearch] = useState('');
  const debouncedTableSearch = useDebounce(tableSearch, 300);
  const [params] = useSearchParams();
  const focusId = Number(params.get('focus'));

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    code: '',
    name: '',
    type: 'PROVINCE',
    parentId: '',
  });
  const { bumpDashboard } = useDashboard();

  const load = async (p = page) => {
    const qTerm = debouncedTableSearch.trim();
    const res = await api.get('/locations', {
      params: {
        page: p,
        size,
        type: filter.type || undefined,
        parentId: filter.parentId || undefined,
        name: filter.name || undefined,
        code: filter.code || undefined,
        sort,
        q: qTerm || undefined,
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

  // Load potential parents when type filter changes to something below PROVINCE
  useEffect(() => {
    const fetchParents = async () => {
      if (!filter.type || filter.type === 'PROVINCE') {
        setParentOptions([]);
        return;
      }
      let parentType;
      if (filter.type === 'DISTRICT') parentType = 'PROVINCE';
      else if (filter.type === 'SECTOR') parentType = 'DISTRICT';
      else if (filter.type === 'CELL') parentType = 'SECTOR';
      else if (filter.type === 'VILLAGE') parentType = 'CELL';
      else parentType = null;

      if (!parentType) {
        setParentOptions([]);
        return;
      }
      const res = await api.get('/locations', {
        params: { type: parentType, page: 0, size: 200 },
      });
      setParentOptions(res.data.content || []);
    };
    fetchParents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter.type]);

  const onSearch = (e) => {
    e.preventDefault();
    load(0);
  };

  const clearFilters = () => {
    setFilter({ type: '', parentId: '', name: '', code: '' });
    setSort('name,asc');
  };

  // live search by type/parent/name (debounced)
  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedFilter, debouncedSort, debouncedTableSearch]);

  const openCreate = () => {
    setEditing(null);
    setForm({
      code: '',
      name: '',
      type: 'PROVINCE',
      parentId: '',
    });
    setIsModalOpen(true);
  };

  const openEdit = async (id) => {
    const res = await api.get(`/locations/${id}`);
    const l = res.data;
    setEditing(l);
    setForm({
      code: l.code || '',
      name: l.name || '',
      type: l.type || 'PROVINCE',
      parentId: l.parentId || '',
    });
    setIsModalOpen(true);
  };

  const save = async () => {
    const payload = {
      code: form.code,
      name: form.name,
      type: form.type,
      parentId: form.parentId || null,
    };
    if (editing && editing.id) {
      await api.put(`/locations/${editing.id}`, payload);
    } else {
      await api.post('/locations', payload);
    }
    setIsModalOpen(false);
    await load(page);
    bumpDashboard();
  };

  const [deleteTarget, setDeleteTarget] = useState(null);

  const requestRemove = (location) => {
    setDeleteTarget(location);
  };

  const performRemove = async () => {
    if (!deleteTarget) return;
    await api.delete(`/locations/${deleteTarget.id}`);
    setDeleteTarget(null);
    await load(page);
    bumpDashboard();
  };

  return (
    <div>
      <div className="section-title">Locations</div>
      <div className="card-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <form onSubmit={onSearch} className="filters">
            <input
              placeholder="Filter by Code"
              value={filter.code}
              onChange={(e) => setFilter({ ...filter, code: e.target.value })}
            />
            <select
              value={filter.type}
              onChange={(e) => setFilter({ ...filter, type: e.target.value })}
            >
              <option value="">Any Type</option>
              <option value="PROVINCE">PROVINCE</option>
              <option value="DISTRICT">DISTRICT</option>
              <option value="SECTOR">SECTOR</option>
              <option value="CELL">CELL</option>
              <option value="VILLAGE">VILLAGE</option>
            </select>
            {/* Parent selector replaced with Typeahead to avoid loading large parent lists */}
            {(() => {
              // Determine which parent TYPE to query for the currently selected child type
              let parentType = null;
              if (!filter.type || filter.type === 'PROVINCE') parentType = null;
              else if (filter.type === 'DISTRICT') parentType = 'PROVINCE';
              else if (filter.type === 'SECTOR') parentType = 'DISTRICT';
              else if (filter.type === 'CELL') parentType = 'SECTOR';
              else if (filter.type === 'VILLAGE') parentType = 'CELL';

              if (!parentType) {
                return (
                  <select value="" onChange={() => { }}>
                    <option value="">Any Parent</option>
                  </select>
                );
              }

              return (
                <Typeahead
                  value={parentOptions.find(p => p.id === filter.parentId) || null}
                  onChange={(opt) => setFilter({ ...filter, parentId: opt ? opt.id : '' })}
                  fetchOptions={async (term) => {
                    const res = await api.get('/locations', { params: { type: parentType, page: 0, size: 10, name: term } });
                    return res.data.content || [];
                  }}
                  getOptionLabel={(o) => o ? `${o.name} (${o.code || ''})` : ''}
                  placeholder="Type parent name..."
                />
              );
            })()}
            <input
              placeholder="Name contains..."
              value={filter.name}
              onChange={(e) =>
                setFilter({ ...filter, name: e.target.value })
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
            <input
              placeholder="🔍 Search by name, code, type..."
              value={tableSearch}
              onChange={(e) => setTableSearch(e.target.value)}
            />
          </form>
          <div style={{ marginTop: '0.5rem' }}>
            <PrimaryButton type="button" onClick={openCreate}>
              New location
            </PrimaryButton>
          </div>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Code</th>
            <th>Name</th>
            <th>Type</th>
            <th>Parent ID</th>
            <th style={{ width: 160 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((l) => (
            <tr key={l.id} className={focusId === l.id ? 'focus-row' : ''}>
              <td>{l.id}</td>
              <td>{l.code}</td>
              <td>{l.name}</td>
              <td>
                <span className="chip">{l.type}</span>
              </td>
              <td>{l.parentId}</td>
              <td>
                <PrimaryButton
                  type="button"
                  variant="secondary"
                  onClick={() => openEdit(l.id)}
                >
                  Edit
                </PrimaryButton>{' '}
                <PrimaryButton
                  type="button"
                  variant="danger"
                  onClick={() => requestRemove(l)}
                >
                  Delete
                </PrimaryButton>
              </td>
            </tr>
          ))}
          {items.length === 0 && (
            <tr>
              <td colSpan={6}>No locations found.</td>
            </tr>
          )}
        </tbody>
      </table>
      <TablePagination page={page} totalPages={totalPages} onPageChange={load} />

      {isModalOpen && (
        <Modal
          title={editing ? 'Edit location' : 'New location'}
          onClose={() => setIsModalOpen(false)}
          primaryLabel="Save"
          onPrimary={save}
          secondaryLabel="Cancel"
        >
          <div className="form-grid">
            <div className="field">
              <label>Code</label>
              <input
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value })}
              />
            </div>
            <div className="field">
              <label>Name</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label>Type</label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
              >
                <option value="PROVINCE">PROVINCE</option>
                <option value="DISTRICT">DISTRICT</option>
                <option value="SECTOR">SECTOR</option>
                <option value="CELL">CELL</option>
                <option value="VILLAGE">VILLAGE</option>
              </select>
            </div>
            <div className="field">
              <label>Parent ID</label>
              <input
                value={form.parentId}
                onChange={(e) => setForm({ ...form, parentId: e.target.value })}
              />
            </div>
          </div>
        </Modal>
      )}

      {deleteTarget && (
        <Modal
          title="Delete location"
          onClose={() => setDeleteTarget(null)}
          primaryLabel="Delete"
          onPrimary={performRemove}
          secondaryLabel="Cancel"
        >
          <p>
            Are you sure you want to delete location{' '}
            <strong>{deleteTarget.name}</strong>?
          </p>
        </Modal>
      )}
    </div>
  );
}

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import PrimaryButton from './PrimaryButton';

export default function Topbar() {
  const { username, role, logout } = useAuth();
  const [q, setQ] = useState('');
  const [results, setResults] = useState([]);
  const [searchError, setSearchError] = useState(null);
  const [searchTimer, setSearchTimer] = useState(null);
  const [searchType, setSearchType] = useState('ALL');
  const [searchById, setSearchById] = useState(false);
  const navigate = useNavigate();

  const performSearch = async (term) => {
    if (!term) {
      setResults([]);
      setSearchError(null);
      return;
    }
    try {
      const res = await api.get('/search', {
        params: {
          q: term,
          type: searchType !== 'ALL' ? searchType : undefined,
          byId: searchById || undefined,
        },
      });
      setResults(res.data || []);
      setSearchError(null);
    } catch (e) {
      setResults([]);
      setSearchError('Search failed. Please try again.');
    }
  };

  const onSearch = async (e) => {
    e.preventDefault();
    performSearch(q);
  };

  const goToProfile = () => {
    navigate('/profile');
  };

  const handleResultClick = (r) => {
    const preferred = r.url;
    let path = '/dashboard';
    if (preferred) {
      path = preferred;
    } else {
      switch (r.type) {
        case 'MEDICINE':
          path = `/medicines?focus=${r.id}`;
          break;
        case 'PRESCRIPTION':
          path = `/prescriptions?focus=${r.id}`;
          break;
        case 'BILL':
          path = `/bills?focus=${r.id}`;
          break;
        case 'LOCATION':
          path = `/locations?focus=${r.id}`;
          break;
        case 'CATEGORY':
          path = `/categories?focus=${r.id}`;
          break;
        case 'PERSON':
          path = role === 'ADMIN' ? `/people?focus=${r.id}` : '/profile';
          break;
        default:
          path = '/dashboard';
      }
    }
    navigate(path, { replace: false });
    setResults([]);
  };

  return (
    <header className="topbar">
      <form onSubmit={onSearch} className="search-form">
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value)}
          style={{ maxWidth: '130px' }}
        >
          <option value="ALL">All types</option>
          <option value="MEDICINE">Medicines</option>
          {role !== 'PATIENT' && <option value="PERSON">People</option>}
          <option value="PRESCRIPTION">Prescriptions</option>
          <option value="BILL">Bills</option>
          {role !== 'PATIENT' && <option value="CATEGORY">Categories</option>}
          {role === 'ADMIN' && <option value="LOCATION">Locations</option>}
        </select>
        <input
          value={q}
          onChange={e => {
            const term = e.target.value;
            setQ(term);
            if (searchTimer) {
              clearTimeout(searchTimer);
            }
            if (searchById) {
              performSearch(term);
              return;
            }
            const handle = setTimeout(() => {
              performSearch(term);
            }, 120);
            setSearchTimer(handle);
          }}
          placeholder="Global search across medicines, people, bills..."
        />
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.25rem',
            fontSize: '0.75rem',
          }}
        >
          <input
            type="checkbox"
            checked={searchById}
            onChange={(e) => setSearchById(e.target.checked)}
          />
          By ID
        </label>
      </form>
      <div className="topbar-user">
        <span
          onClick={goToProfile}
          style={{
            cursor: 'pointer',
            textDecoration: 'underline',
            color: '#10b981',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '0.25rem'
          }}
          title="Go to Profile"
        >
          👤 {username}
        </span>
        <PrimaryButton type="button" variant="secondary" onClick={logout}>
          Logout
        </PrimaryButton>
      </div>
      {(results.length > 0 || searchError) && (
        <div className="search-results">
          {searchError && <div className="search-item-error">{searchError}</div>}
          {results.map(r => (
            <button
              key={`${r.type}-${r.id}`}
              type="button"
              className="search-item"
              onClick={() => handleResultClick(r)}
            >
              <span className="badge">{r.type}</span>
              <div>
                <div>{r.label}</div>
                {r.subtitle && <small>{r.subtitle}</small>}
                {!r.subtitle && r.description && <small>{r.description}</small>}
              </div>
            </button>
          ))}
        </div>
      )}
    </header>
  );
}

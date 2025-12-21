import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [role, setRole] = useState(localStorage.getItem('role'));
  const [email, setEmail] = useState(localStorage.getItem('email'));
  const [username, setUsername] = useState(localStorage.getItem('username'));

  useEffect(() => {
    if (token) localStorage.setItem('token', token); else localStorage.removeItem('token');
    if (role) localStorage.setItem('role', role); else localStorage.removeItem('role');
    if (email) localStorage.setItem('email', email); else localStorage.removeItem('email');
    if (username) localStorage.setItem('username', username); else localStorage.removeItem('username');
  }, [token, role, email, username]);

  const login = (t, r, e, u) => {
    setToken(t);
    setRole(r);
    setEmail(e);
    setUsername(u || e?.split('@')[0]); // fallback to email prefix if no username
  };

  const logout = () => {
    setToken(null);
    setRole(null);
    setEmail(null);
    setUsername(null);
  };

  return (
    <AuthContext.Provider value={{ token, role, email, username, setUsername, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

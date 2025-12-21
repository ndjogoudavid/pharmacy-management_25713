import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { role } = useAuth();
  const links = [
    { to: '/dashboard', label: 'Dashboard', roles: ['ADMIN','PHARMACIST','PATIENT'] },
    { to: '/profile', label: 'Profile', roles: ['ADMIN','PHARMACIST','PATIENT'] },
    { to: '/medicines', label: 'Medicines', roles: ['ADMIN','PHARMACIST'] },
    { to: '/categories', label: 'Categories', roles: ['ADMIN','PHARMACIST'] },
    { to: '/prescriptions', label: 'Prescriptions', roles: ['ADMIN','PHARMACIST','PATIENT'] },
    { to: '/people', label: 'People', roles: ['ADMIN'] },
    { to: '/bills', label: 'Bills', roles: ['ADMIN','PHARMACIST','PATIENT'] },
    { to: '/locations', label: 'Locations', roles: ['ADMIN'] },
  ];
  return (
    <aside className="sidebar">
      <div className="sidebar-title">Pharmacy</div>
      <nav>
        {links
          .filter((l) => !role || l.roles.includes(role))
          .map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                ['nav-link', isActive ? 'active' : ''].filter(Boolean).join(' ')
              }
            >
              {link.label}
            </NavLink>
          ))}
      </nav>
    </aside>
  );
}

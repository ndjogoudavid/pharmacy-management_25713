import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Medicines from './pages/Medicines';
import People from './pages/People';
import Bills from './pages/Bills';
import BillReceiptPage from './pages/BillReceiptPage';
import Locations from './pages/Locations';
import Categories from './pages/Categories';
import Profile from './pages/Profile';
import Login from './pages/auth/Login';
import Verify2FA from './pages/auth/Verify2FA';
import ForgotPassword from './pages/auth/ForgotPassword';
import ResetPassword from './pages/auth/ResetPassword';
import { AuthProvider, useAuth } from './context/AuthContext';
import { DashboardProvider } from './context/DashboardContext';
import PrescriptionsListPage from './pages/PrescriptionsListPage';
import PrescriptionDetailPage from './pages/PrescriptionDetailPage';
import PrescriptionFormPage from './pages/PrescriptionFormPage';

const PrivateRoute = ({ children, roles }) => {
  const { token, role } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(role)) return <Navigate to="/dashboard" replace />;
  return <Layout>{children}</Layout>;
};

export default function App() {
  return (
    <AuthProvider>
      <DashboardProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/verify-2fa" element={<Verify2FA />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/profile" element={<PrivateRoute><Profile /></PrivateRoute>} />
          <Route path="/medicines" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST']}><Medicines /></PrivateRoute>} />
          <Route path="/categories" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST']}><Categories /></PrivateRoute>} />
          <Route path="/prescriptions" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST', 'PATIENT']}><PrescriptionsListPage /></PrivateRoute>} />
          <Route path="/prescriptions/new" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST']}><PrescriptionFormPage /></PrivateRoute>} />
          <Route path="/prescriptions/:id" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST', 'PATIENT']}><PrescriptionDetailPage /></PrivateRoute>} />
          <Route path="/prescriptions/:id/edit" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST']}><PrescriptionFormPage /></PrivateRoute>} />
          <Route path="/people" element={<PrivateRoute roles={['ADMIN']}><People /></PrivateRoute>} />
          <Route path="/bills" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST', 'PATIENT']}><Bills /></PrivateRoute>} />
          <Route path="/bills/:id/receipt" element={<PrivateRoute roles={['ADMIN', 'PHARMACIST', 'PATIENT']}><BillReceiptPage /></PrivateRoute>} />
          <Route path="/locations" element={<PrivateRoute roles={['ADMIN']}><Locations /></PrivateRoute>} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </DashboardProvider>
    </AuthProvider>
  );
}

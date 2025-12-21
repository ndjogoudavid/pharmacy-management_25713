import { createContext, useContext, useState, useCallback } from 'react';

const DashboardContext = createContext(null);

export function DashboardProvider({ children }) {
  const [version, setVersion] = useState(0);

  const bumpDashboard = useCallback(() => {
    setVersion((v) => v + 1);
  }, []);

  return (
    <DashboardContext.Provider value={{ version, bumpDashboard }}>
      {children}
    </DashboardContext.Provider>
  );
}

export function useDashboard() {
  const ctx = useContext(DashboardContext);
  if (!ctx) {
    throw new Error('useDashboard must be used within DashboardProvider');
  }
  return ctx;
}


import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Prescriptions() {
  const navigate = useNavigate();

  useEffect(() => {
    // Redirect legacy import to the new list page
    navigate('/prescriptions', { replace: true });
  }, [navigate]);

  return null;
}

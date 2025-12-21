import { useEffect, useState } from 'react';

// Simple value debouncer. Returns a debounced version of the value
export default function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [JSON.stringify(value), delay]); // Use JSON.stringify to detect object changes

  return debounced;
}


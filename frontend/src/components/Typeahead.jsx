import { useEffect, useState } from 'react';
import useDebounce from '../hooks/useDebounce';

export default function Typeahead({
  value,
  onChange, // receives selected option object or null
  fetchOptions, // async function(term) => Array<obj>
  placeholder = '',
  getOptionLabel = (o) => o?.name || o?.label || String(o?.id || ''),
  minLength = 1,
  disabled = false,
}) {
  const [input, setInput] = useState(value ? getOptionLabel(value) : '');
  const debounced = useDebounce(input, 300);
  const [options, setOptions] = useState([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!debounced || debounced.length < minLength) {
      setOptions([]);
      return;
    }

    let mounted = true;
    (async () => {
      try {
        const res = await fetchOptions(debounced);
        if (mounted) setOptions(res || []);
      } catch (e) {
        if (mounted) setOptions([]);
      }
    })();

    return () => (mounted = false);
  }, [debounced, fetchOptions, minLength]);

  useEffect(() => {
    // update input when external value changes
    setInput(value ? getOptionLabel(value) : '');
  }, [value, getOptionLabel]);

  const pick = (opt) => {
    setInput(getOptionLabel(opt));
    setOpen(false);
    onChange && onChange(opt);
  };

  return (
    <div className="typeahead">
      <input
        value={input}
        onChange={(e) => {
          if (disabled) return;
          setInput(e.target.value);
          setOpen(true);
          if (e.target.value === '') onChange && onChange(null);
        }}
        onFocus={() => !disabled && setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder={placeholder}
        aria-autocomplete="list"
        disabled={disabled}
      />
      {open && options && options.length > 0 && (
        <ul className="typeahead-options">
          {options.map((opt) => (
            <li
              key={opt.id}
              onMouseDown={() => pick(opt)}
              className="typeahead-option"
            >
              {getOptionLabel(opt)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

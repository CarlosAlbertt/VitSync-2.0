import { useEffect, useState } from "react";

/**
 * El valor, pero solo cuando lleva `delayMs` sin cambiar. Para búsquedas mientras se escribe:
 * contra la API real, sin esto cada tecla sería una petición.
 */
export function useDebouncedValue<T>(value: T, delayMs = 250): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
}

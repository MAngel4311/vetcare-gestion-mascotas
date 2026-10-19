import { useEffect, useState } from 'react';

// Carga datos al entrar a la pantalla y cada vez que cambian las dependencias.
export function useLoad(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let vivo = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    fn()
      .then((data) => vivo && setState({ data, loading: false, error: null }))
      .catch((error) => vivo && setState({ data: null, loading: false, error }));
    return () => { vivo = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  return { ...state, reload: () => setTick((t) => t + 1) };
}

// Estado de un formulario: v = valores, set(nombre, valor), bind(nombre) para los inputs.
export function useForm(inicial) {
  const [v, setV] = useState(inicial);
  const set = (k, val) => setV((s) => ({ ...s, [k]: val }));
  const bind = (k) => ({ value: v[k] ?? '', onChange: (e) => set(k, e.target.value) });
  return { v, set, bind };
}

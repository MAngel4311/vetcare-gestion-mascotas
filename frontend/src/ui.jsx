import { useEffect, useState } from 'react';
import { ESTADOS } from './api';

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <header>
          <h2>{title}</h2>
          <button className="icon" onClick={onClose} aria-label="Cerrar">✕</button>
        </header>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

export function Field({ label, error, children, hint }) {
  return (
    <label className={`field ${error ? 'has-error' : ''}`}>
      <span>{label}</span>
      {children}
      {hint && !error && <small className="hint">{hint}</small>}
      {error && <small className="err">{error}</small>}
    </label>
  );
}

// Muestra el mensaje del backend. Los errores por campo se ven bajo cada input (ver prop error de Field).
export function ErrorBox({ error }) {
  if (!error) return null;
  const hayCampos = Object.keys(error.fieldErrors || {}).length > 0;
  return (
    <div className="alert error" role="alert">
      {hayCampos ? 'Revisa los campos marcados.' : error.message}
    </div>
  );
}

export const Cargando = () => <p className="muted">Cargando…</p>;
export const Vacio = ({ children }) => <div className="empty">{children}</div>;

export function Estado({ estado }) {
  return <span className={`badge ${estado}`}>{ESTADOS[estado] || estado}</span>;
}

export function Titulo({ children, accion }) {
  return (
    <div className="page-head">
      <h1>{children}</h1>
      {accion}
    </div>
  );
}

// Aviso temporal (confirmaciones de éxito).
export function useAviso() {
  const [msg, setMsg] = useState('');
  useEffect(() => {
    if (!msg) return undefined;
    const t = setTimeout(() => setMsg(''), 4000);
    return () => clearTimeout(t);
  }, [msg]);
  const el = msg ? <div className="alert ok" role="status">{msg}</div> : null;
  return [setMsg, el];
}

// Hook para enviar formularios con estado de "guardando" y error.
export function useSubmit(accion, alTerminar) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const enviar = async (e) => {
    e?.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await accion();
      alTerminar?.(r);
    } catch (ex) {
      setError(ex);
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, enviar };
}

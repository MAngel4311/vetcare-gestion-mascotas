// Cliente HTTP único. Todas las pantallas hablan con el backend a través de este archivo.
const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

let token = localStorage.getItem('vc_token');
let onUnauthorized = () => {};

export function setToken(t) {
  token = t;
  if (t) localStorage.setItem('vc_token', t);
  else localStorage.removeItem('vc_token');
}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== '' && v != null) p.set(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
};

async function request(method, path, body) {
  let res;
  try {
    res = await fetch(BASE + path, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, `No se pudo conectar con el servidor (${BASE}). Verifica que el backend esté en ejecución.`);
  }
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && path !== '/api/auth/login') onUnauthorized();
    throw new ApiError(res.status, data?.message || 'Ocurrió un error inesperado.', data?.fieldErrors);
  }
  return data;
}

export const api = {
  get: (path, params) => request('GET', path + qs(params)),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body),
  patch: (path, body) => request('PATCH', path, body ?? {}),
};

// Descarga un archivo (p. ej. el CSV del reporte) enviando el token; fetch normal no puede usar un enlace <a>.
export async function descargar(path, params, nombreArchivo) {
  let res;
  try {
    res = await fetch(BASE + path + qs(params), { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  } catch {
    throw new ApiError(0, `No se pudo conectar con el servidor (${BASE}). Verifica que el backend esté en ejecución.`);
  }
  if (!res.ok) {
    const data = await res.json().catch(() => null);
    if (res.status === 401) onUnauthorized();
    throw new ApiError(res.status, data?.message || 'No se pudo descargar el archivo.', data?.fieldErrors);
  }
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement('a');
  a.href = url;
  a.download = nombreArchivo;
  a.click();
  URL.revokeObjectURL(url);
}

// Descarga un archivo (por ejemplo el CSV del reporte) enviando el token.
api.download = async (path, params, nombre) => {
  let res;
  try {
    res = await fetch(BASE + path + qs(params), { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  } catch {
    throw new ApiError(0, `No se pudo conectar con el servidor (${BASE}). Verifica que el backend esté en ejecución.`);
  }
  if (!res.ok) {
    const d = await res.json().catch(() => null);
    if (res.status === 401) onUnauthorized();
    throw new ApiError(res.status, d?.message || 'No se pudo descargar el archivo.');
  }
  const blob = await res.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = nombre;
  a.click();
  URL.revokeObjectURL(a.href);
};

// ---------- utilidades de fecha (se trabaja con texto para evitar problemas de zona horaria) ----------
export const hoy = () => {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 10);
};
export const hora = (iso) => (iso ? iso.slice(11, 16) : '');
export const minutos = (hhmm) => { const [h, m] = hhmm.split(':').map(Number); return h * 60 + m; };
export const sumarMinutos = (fecha, hhmm, min) => {
  const t = Math.min(minutos(hhmm) + Number(min), 1439);
  const h = String(Math.floor(t / 60)).padStart(2, '0');
  const m = String(t % 60).padStart(2, '0');
  return `${fecha}T${h}:${m}:00`;
};
export const duracion = (inicio, fin) => minutos(hora(fin)) - minutos(hora(inicio));
export const fmtFecha = (f) => {
  if (!f) return '—';
  const [y, m, d] = f.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
};
export const fmtFechaHora = (iso) => `${fmtFecha(iso)}, ${hora(iso)}`;

export const MOTIVOS = { CONTROL: 'Control', VACUNACION: 'Vacunación', URGENCIA: 'Urgencia', OTRO: 'Otro' };
export const ESTADOS = { PROGRAMADA: 'Pendiente', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada' };
export const ROLES = { ADMINISTRADOR: 'Administrador', VETERINARIO: 'Veterinario', RECEPCIONISTA: 'Recepcionista', USUARIO: 'Dueño' };

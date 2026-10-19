import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, setToken, setUnauthorizedHandler } from './api';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('vc_user')); } catch { return null; }
  });

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem('vc_user');
    setUser(null);
  }, []);

  // Si el token vence o es inválido (401), se cierra la sesión automáticamente.
  useEffect(() => { setUnauthorizedHandler(logout); }, [logout]);

  const iniciarSesion = (r) => {
    setToken(r.token);
    const u = { username: r.username, nombreCompleto: r.nombreCompleto, rol: r.rol, duenoId: r.duenoId };
    localStorage.setItem('vc_user', JSON.stringify(u));
    setUser(u);
  };
  const login = async (username, password) => iniciarSesion(await api.post('/api/auth/login', { username, password }));
  // Un dueño crea su propia cuenta (crea su ficha de dueño y entra directamente).
  const registro = async (datos) => iniciarSesion(await api.post('/api/auth/registro', datos));

  const rol = user?.rol;
  // Misma matriz de permisos que el backend (SecurityConfig). El backend siempre valida; esto solo oculta botones.
  const can = {
    clientes: rol === 'RECEPCIONISTA' || rol === 'ADMINISTRADOR', // dueños y citas
    clinico: rol === 'VETERINARIO' || rol === 'ADMINISTRADOR',    // consulta clínica y registrar vacunas
    admin: rol === 'ADMINISTRADOR',                               // reportes, veterinarios, usuarios
    dueno: rol === 'USUARIO',                                     // dueño: solo ve lo suyo
    mascotas: rol === 'RECEPCIONISTA' || rol === 'ADMINISTRADOR' || rol === 'USUARIO',
  };

  return <Ctx.Provider value={{ user, login, registro, logout, can }}>{children}</Ctx.Provider>;
}

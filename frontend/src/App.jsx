import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { BarChart3, CalendarDays, CircleUser, LogOut, PawPrint, ShieldCheck, Stethoscope, Syringe, Users } from 'lucide-react';
import { useAuth } from './auth';
import { ROLES } from './api';
import logo from './assets/logo-completo.png';
import Login from './pages/Login';
import Agenda from './pages/Agenda';
import Mascotas from './pages/Mascotas';
import Duenos from './pages/Duenos';
import VacunasProximas from './pages/VacunasProximas';
import Reportes from './pages/Reportes';
import Veterinarios from './pages/Veterinarios';
import Usuarios from './pages/Usuarios';
import Perfil from './pages/Perfil';

// Enlace del menú con su ícono (solo presentación).
const Item = ({ to, icon: Icono, children }) => (
  <NavLink to={to} className={({ isActive }) => (isActive ? 'active' : undefined)}>
    <Icono aria-hidden="true" />
    {children}
  </NavLink>
);

const iniciales = (nombre = '') => nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();

function Layout({ children }) {
  const { user, logout, can } = useAuth();
  const { pathname } = useLocation();
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand"><img src={logo} alt="VetCare — Software de gestión" /></div>
        <nav>
          {can.dueno ? (
            <>
              <Item to="/mascotas" icon={PawPrint}>Mis mascotas</Item>
              <Item to="/agenda" icon={CalendarDays}>Mis citas</Item>
              <Item to="/perfil" icon={CircleUser}>Mi cuenta</Item>
            </>
          ) : (
            <>
              <Item to="/agenda" icon={CalendarDays}>Agenda de citas</Item>
              <Item to="/mascotas" icon={PawPrint}>Mascotas</Item>
              <Item to="/duenos" icon={Users}>Dueños</Item>
              <Item to="/vacunas" icon={Syringe}>Vacunas próximas</Item>
              {can.admin && (
                <>
                  <span className="nav-group">Administración</span>
                  <Item to="/reportes" icon={BarChart3}>Reportes</Item>
                  <Item to="/veterinarios" icon={Stethoscope}>Veterinarios</Item>
                  <Item to="/usuarios" icon={ShieldCheck}>Usuarios</Item>
                </>
              )}
              <span className="nav-group">Sesión</span>
              <Item to="/perfil" icon={CircleUser}>Mi cuenta</Item>
            </>
          )}
        </nav>
        <div className="me">
          <div className="avatar" aria-hidden="true">{iniciales(user.nombreCompleto)}</div>
          <strong>{user.nombreCompleto}</strong>
          <span className="rol">{ROLES[user.rol]}</span>
          <button className="ghost" onClick={logout}><LogOut aria-hidden="true" /> Cerrar sesión</button>
        </div>
      </aside>
      {/* La clave reinicia la animación de entrada en cada pantalla */}
      <main className="main"><div className="page" key={pathname}>{children}</div></main>
    </div>
  );
}

// Solo deja pasar a quien tenga el permiso; si no, vuelve a la pantalla de inicio.
const Solo = ({ ok, children }) => (ok ? children : <Navigate to="/agenda" replace />);

export default function App() {
  const { user, can } = useAuth();
  if (!user) {
    return (
      <Routes>
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }
  return (
    <Layout>
      <Routes>
        <Route path="/agenda" element={<Agenda />} />
        <Route path="/mascotas" element={<Mascotas />} />
        <Route path="/perfil" element={<Perfil />} />
        <Route path="/duenos" element={<Solo ok={!can.dueno}><Duenos /></Solo>} />
        <Route path="/vacunas" element={<Solo ok={!can.dueno}><VacunasProximas /></Solo>} />
        <Route path="/reportes" element={<Solo ok={can.admin}><Reportes /></Solo>} />
        <Route path="/veterinarios" element={<Solo ok={can.admin}><Veterinarios /></Solo>} />
        <Route path="/usuarios" element={<Solo ok={can.admin}><Usuarios /></Solo>} />
        <Route path="*" element={<Navigate to={can.dueno ? '/mascotas' : '/agenda'} replace />} />
      </Routes>
    </Layout>
  );
}

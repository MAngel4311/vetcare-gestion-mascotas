import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import { ROLES } from './api';
import Login from './pages/Login';
import Agenda from './pages/Agenda';
import Mascotas from './pages/Mascotas';
import Duenos from './pages/Duenos';
import VacunasProximas from './pages/VacunasProximas';
import Reportes from './pages/Reportes';
import Veterinarios from './pages/Veterinarios';
import Usuarios from './pages/Usuarios';
import Perfil from './pages/Perfil';

function Layout({ children }) {
  const { user, logout, can } = useAuth();
  const link = ({ isActive }) => (isActive ? 'active' : undefined);
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">VetCare</div>
        <nav>
          {can.dueno ? (
            <>
              <NavLink to="/mascotas" className={link}>Mis mascotas</NavLink>
              <NavLink to="/agenda" className={link}>Mis citas</NavLink>
              <NavLink to="/perfil" className={link}>Mi cuenta</NavLink>
            </>
          ) : (
            <>
              <NavLink to="/agenda" className={link}>Agenda de citas</NavLink>
              <NavLink to="/mascotas" className={link}>Mascotas</NavLink>
              <NavLink to="/duenos" className={link}>Dueños</NavLink>
              <NavLink to="/vacunas" className={link}>Vacunas próximas</NavLink>
              {can.admin && (
                <>
                  <span className="nav-group">Administración</span>
                  <NavLink to="/reportes" className={link}>Reportes</NavLink>
                  <NavLink to="/veterinarios" className={link}>Veterinarios</NavLink>
                  <NavLink to="/usuarios" className={link}>Usuarios</NavLink>
                </>
              )}
              <span className="nav-group">Sesión</span>
              <NavLink to="/perfil" className={link}>Mi cuenta</NavLink>
            </>
          )}
        </nav>
        <div className="me">
          <strong>{user.nombreCompleto}</strong>
          <span>{ROLES[user.rol]}</span>
          <button className="ghost" onClick={logout}>Cerrar sesión</button>
        </div>
      </aside>
      <main className="main">{children}</main>
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

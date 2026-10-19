import { useMemo, useState } from 'react';
import { useAuth } from '../auth';
import { useForm } from '../hooks';
import { ErrorBox, Field, useSubmit } from '../ui';
import ElectricLogo from '../components/ElectricLogo';
import logoCompleto from '../assets/logo-completo.png';
import logoMarca from '../assets/logo-marca.png';
import silueta from '../assets/logo-silueta.png';

// La animación usa WebGL. Si el equipo no lo soporta, o la persona pidió «reducir movimiento», se muestra el logo fijo.
function puedeAnimar() {
  try {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false;
    return !!document.createElement('canvas').getContext('webgl2');
  } catch {
    return false;
  }
}

// Panel de bienvenida (solo presentación).
function Bienvenida() {
  const animar = useMemo(puedeAnimar, []);
  return (
    <aside className="login-hero" aria-hidden="false">
      {animar ? (
        <div className="anim">
          <ElectricLogo
            src={silueta}
            color="#d6fff4"
            glowColor="#2bb3c9"
            theme="dark"
            scale={0.62}
            intensity={1.1}
            glow={1.15}
            thickness={1.4}
            strands={4}
            speed={2}
          />
        </div>
      ) : (
        <img className="estatico" src={logoMarca} alt="" />
      )}
      <div className="texto">
        <h2>Cuidamos a quienes más quieres, con toda la información en un solo lugar.</h2>
        <p>Agenda, historial clínico y control de vacunas para tu clínica veterinaria.</p>
        <ul>
          <li>Citas sin choques de horario</li>
          <li>Historial de cada mascota</li>
          <li>Recordatorios de vacunas</li>
        </ul>
      </div>
    </aside>
  );
}

function Registro({ volver }) {
  const { registro } = useAuth();
  const { v, bind } = useForm({ nombre: '', documento: '', telefono: '', email: '', direccion: '', username: '', password: '' });
  const { busy, error, enviar } = useSubmit(() => registro({ ...v, email: v.email.trim() || null }));
  const fe = error?.fieldErrors || {};
  return (
    <form className="login-card wide" onSubmit={enviar}>
      <img className="logo" src={logoCompleto} alt="VetCare" />
      <h1>Crear cuenta de dueño</h1>
      <p className="muted">Registra tus datos para consultar a tus mascotas, sus citas y vacunas.</p>
      <Field label="Nombre completo" error={fe.nombre}><input {...bind('nombre')} /></Field>
      <div className="grid2">
        <Field label="Documento" error={fe.documento}><input {...bind('documento')} /></Field>
        <Field label="Teléfono" error={fe.telefono} hint="10 dígitos"><input {...bind('telefono')} inputMode="numeric" /></Field>
      </div>
      <Field label="Correo electrónico" error={fe.email} hint="Aquí te llegan confirmaciones y recordatorios"><input type="email" {...bind('email')} /></Field>
      <Field label="Dirección" error={fe.direccion}><input {...bind('direccion')} /></Field>
      <div className="grid2">
        <Field label="Usuario" error={fe.username} hint="3 a 50 caracteres"><input {...bind('username')} autoComplete="username" /></Field>
        <Field label="Contraseña" error={fe.password} hint="Mínimo 8 caracteres"><input type="password" {...bind('password')} autoComplete="new-password" /></Field>
      </div>
      <ErrorBox error={error} />
      <button className="primary block" disabled={busy}>{busy ? 'Creando cuenta…' : 'Crear cuenta'}</button>
      <button type="button" className="link" onClick={volver}>Ya tengo cuenta</button>
    </form>
  );
}

export default function Login() {
  const { login } = useAuth();
  const [modo, setModo] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { busy, error, enviar } = useSubmit(() => login(username.trim(), password));

  return (
    <div className="login">
      <Bienvenida />
      <section className="login-panel">
        {modo === 'registro' ? <Registro volver={() => setModo('login')} /> : (
          <form className="login-card" onSubmit={enviar}>
            <img className="logo" src={logoCompleto} alt="VetCare" />
            <h1>Bienvenido de nuevo</h1>
            <p className="muted">Ingresa con tu usuario para gestionar citas, mascotas e historiales.</p>
            <label className="field">
              <span>Usuario</span>
              <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus autoComplete="username" required />
            </label>
            <label className="field">
              <span>Contraseña</span>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
            </label>
            <ErrorBox error={error} />
            <button className="primary block" disabled={busy}>{busy ? 'Ingresando…' : 'Iniciar sesión'}</button>
            <button type="button" className="link" onClick={() => setModo('registro')}>¿Eres dueño de una mascota? Crea tu cuenta</button>
          </form>
        )}
      </section>
    </div>
  );
}

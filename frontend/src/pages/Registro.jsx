import { Link } from 'react-router-dom';
import { useAuth } from '../auth';
import { useForm } from '../hooks';
import { ErrorBox, Field, useSubmit } from '../ui';

// Registro propio del dueño de mascotas: crea su ficha y su cuenta (rol Dueño) y entra directamente.
export default function Registro() {
  const { registro } = useAuth();
  const { v, bind } = useForm({ username: '', password: '', nombre: '', documento: '', telefono: '', email: '', direccion: '' });
  const { busy, error, enviar } = useSubmit(() => registro({ ...v, email: v.email.trim() || null }));
  const fe = error?.fieldErrors || {};
  return (
    <div className="login">
      <form className="login-card wide" onSubmit={enviar}>
        <h1>Crear cuenta de dueño</h1>
        <p className="muted">Regístrate para ver tus mascotas, sus citas, vacunas e historial clínico.</p>
        <Field label="Nombre completo" error={fe.nombre}><input {...bind('nombre')} autoFocus /></Field>
        <div className="grid2">
          <Field label="Documento" error={fe.documento}><input {...bind('documento')} /></Field>
          <Field label="Teléfono" error={fe.telefono} hint="10 dígitos"><input {...bind('telefono')} inputMode="numeric" /></Field>
        </div>
        <Field label="Correo electrónico" error={fe.email} hint="Aquí recibirás confirmaciones y recordatorios"><input type="email" {...bind('email')} /></Field>
        <Field label="Dirección" error={fe.direccion}><input {...bind('direccion')} /></Field>
        <div className="grid2">
          <Field label="Usuario" error={fe.username} hint="Letras, números, punto o guion"><input {...bind('username')} autoComplete="username" /></Field>
          <Field label="Contraseña" error={fe.password} hint="Mínimo 8 caracteres"><input type="password" {...bind('password')} autoComplete="new-password" /></Field>
        </div>
        <ErrorBox error={error} />
        <button className="primary block" disabled={busy}>{busy ? 'Creando cuenta…' : 'Crear cuenta'}</button>
        <p className="muted center">¿Ya tienes cuenta? <Link to="/">Inicia sesión</Link></p>
      </form>
    </div>
  );
}

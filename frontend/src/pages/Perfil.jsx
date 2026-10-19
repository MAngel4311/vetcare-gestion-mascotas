import { useState } from 'react';
import { api, ROLES } from '../api';
import { useAuth } from '../auth';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Titulo, useAviso, useSubmit } from '../ui';
import { DuenoForm } from './Duenos';

function CambiarPassword() {
  const { v, set, bind } = useForm({ actual: '', nueva: '', repetir: '' });
  const [local, setLocal] = useState('');
  const [aviso, avisoEl] = useAviso();
  const { busy, error, enviar } = useSubmit(async () => {
    setLocal('');
    if (v.nueva !== v.repetir) { setLocal('Las contraseñas nuevas no coinciden.'); return false; }
    await api.put('/api/auth/password', { actual: v.actual, nueva: v.nueva });
    return true;
  }, (ok) => {
    if (ok) { set('actual', ''); set('nueva', ''); set('repetir', ''); aviso('Contraseña actualizada.'); }
  });
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form card">
      <h2>Cambiar contraseña</h2>
      {avisoEl}
      <Field label="Contraseña actual" error={fe.actual}><input type="password" autoComplete="current-password" {...bind('actual')} /></Field>
      <div className="grid2">
        <Field label="Contraseña nueva" error={fe.nueva} hint="Mínimo 8 caracteres"><input type="password" autoComplete="new-password" {...bind('nueva')} /></Field>
        <Field label="Repite la nueva"><input type="password" autoComplete="new-password" {...bind('repetir')} /></Field>
      </div>
      {local && <div className="alert error" role="alert">{local}</div>}
      <ErrorBox error={error} />
      <div className="actions"><button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Cambiar contraseña'}</button></div>
    </form>
  );
}

function MisDatos({ duenoId }) {
  const [aviso, avisoEl] = useAviso();
  const { data, loading, error } = useLoad(() => api.get(`/api/duenos/${duenoId}`));
  return (
    <section className="card">
      <h2>Mis datos</h2>
      {avisoEl}
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && <DuenoForm dueno={data} onSaved={() => aviso('Datos actualizados.')} />}
    </section>
  );
}

export default function Perfil() {
  const { user, can } = useAuth();
  return (
    <>
      <Titulo>Mi cuenta</Titulo>
      <section className="card">
        <dl className="datos">
          <dt>Nombre</dt><dd>{user.nombreCompleto}</dd>
          <dt>Usuario</dt><dd>{user.username}</dd>
          <dt>Rol</dt><dd>{ROLES[user.rol]}</dd>
        </dl>
      </section>
      {can.dueno && user.duenoId && <MisDatos duenoId={user.duenoId} />}
      <CambiarPassword />
    </>
  );
}

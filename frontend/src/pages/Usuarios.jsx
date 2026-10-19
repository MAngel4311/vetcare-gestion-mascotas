import { useState } from 'react';
import { api, ROLES } from '../api';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Modal, Titulo, useAviso, useSubmit } from '../ui';

function UsuarioForm({ usuario, onSaved, onCancel }) {
  const { v, set, bind } = useForm({
    username: usuario?.username || '', password: '', nombreCompleto: usuario?.nombreCompleto || '',
    rol: usuario?.rol || 'RECEPCIONISTA', activo: usuario ? usuario.activo : true, duenoId: '',
  });
  // Para crear una cuenta de dueño hay que elegir a qué dueño pertenece.
  const { data: duenos } = useLoad(() => (usuario ? Promise.resolve([]) : api.get('/api/duenos')));
  const { busy, error, enviar } = useSubmit(() => (usuario
    ? api.put(`/api/usuarios/${usuario.id}`, { nombreCompleto: v.nombreCompleto, rol: v.rol, activo: v.activo, password: v.password || null })
    : api.post('/api/usuarios', { username: v.username, password: v.password, nombreCompleto: v.nombreCompleto, rol: v.rol, duenoId: v.rol === 'USUARIO' && v.duenoId !== '' ? Number(v.duenoId) : null })), onSaved);
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <div className="grid2">
        <Field label="Usuario" error={fe.username} hint={usuario ? 'No se puede cambiar' : undefined}><input {...bind('username')} disabled={!!usuario} /></Field>
        <Field label={usuario ? 'Nueva contraseña' : 'Contraseña'} error={fe.password} hint={usuario ? 'Déjala vacía para no cambiarla' : 'Mínimo 8 caracteres'}>
          <input type="password" autoComplete="new-password" {...bind('password')} />
        </Field>
      </div>
      <Field label="Nombre completo" error={fe.nombreCompleto}><input {...bind('nombreCompleto')} /></Field>
      <Field label="Rol" error={fe.rol}>
        <select {...bind('rol')}>{Object.entries(ROLES).map(([k, t]) => <option key={k} value={k}>{t}</option>)}</select>
      </Field>
      {!usuario && v.rol === 'USUARIO' && (
        <Field label="Dueño asociado" error={fe.duenoId} hint="La cuenta verá solo las mascotas y citas de este dueño">
          <select {...bind('duenoId')}>
            <option value="">Selecciona un dueño…</option>
            {(duenos || []).map((d) => <option key={d.id} value={d.id}>{d.nombre} — {d.documento}</option>)}
          </select>
        </Field>
      )}
      {usuario && <label className="check"><input type="checkbox" checked={v.activo} onChange={(e) => set('activo', e.target.checked)} /> Usuario activo</label>}
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar usuario'}</button>
      </div>
    </form>
  );
}

export default function Usuarios() {
  const [modal, setModal] = useState(null);
  const [aviso, avisoEl] = useAviso();
  const { data, loading, error, reload } = useLoad(() => api.get('/api/usuarios'));
  return (
    <>
      <Titulo accion={<button className="primary" onClick={() => setModal('nuevo')}>Nuevo usuario</button>}>Usuarios</Titulo>
      {avisoEl}
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {data.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.username}</strong></td><td>{u.nombreCompleto}</td><td>{ROLES[u.rol]}</td>
                  <td><span className={`badge ${u.activo ? 'ATENDIDA' : 'CANCELADA'}`}>{u.activo ? 'Activo' : 'Inactivo'}</span></td>
                  <td className="right"><button onClick={() => setModal(u)}>Editar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal && (
        <Modal title={modal === 'nuevo' ? 'Nuevo usuario' : 'Editar usuario'} onClose={() => setModal(null)}>
          <UsuarioForm usuario={modal === 'nuevo' ? null : modal} onCancel={() => setModal(null)} onSaved={() => { setModal(null); aviso('Usuario guardado.'); reload(); }} />
        </Modal>
      )}
    </>
  );
}

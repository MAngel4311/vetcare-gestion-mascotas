import { useState } from 'react';
import { api } from '../api';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Modal, Titulo, Vacio, useAviso, useSubmit } from '../ui';

// Al crear un veterinario, el backend le abre una cuenta y devuelve su clave temporal UNA sola vez.
function CredencialesVet({ v, onClose }) {
  const [copiado, setCopiado] = useState(false);
  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(`Usuario: ${v.usuario}\nContraseña temporal: ${v.passwordTemporal}`);
      setCopiado(true);
    } catch { /* se puede copiar a mano */ }
  };
  return (
    <div className="cred">
      <p><strong>{v.nombre}</strong> quedó registrado. Entrégale estos datos de acceso{v.email ? ' (también se los enviamos por correo)' : ''}:</p>
      <dl>
        <dt>Usuario</dt><dd><code>{v.usuario}</code></dd>
        <dt>Contraseña temporal</dt><dd><code>{v.passwordTemporal}</code></dd>
      </dl>
      <div className="alert error">Anótala ahora: no se volverá a mostrar. Puede cambiarla en «Mi cuenta».</div>
      <div className="actions">
        <button onClick={copiar}>{copiado ? 'Copiado ✓' : 'Copiar'}</button>
        <button className="primary" onClick={onClose}>Entendido</button>
      </div>
    </div>
  );
}

function VetForm({ vet, onSaved, onCancel }) {
  const { v, set, bind } = useForm({
    nombre: vet?.nombre || '', documento: vet?.documento || '', telefono: vet?.telefono || '',
    email: vet?.email || '', especialidad: vet?.especialidad || '', activo: vet ? vet.activo : true,
  });
  const { busy, error, enviar } = useSubmit(() => {
    const body = { ...v, email: v.email.trim() || null, especialidad: v.especialidad.trim() || null };
    return vet ? api.put(`/api/veterinarios/${vet.id}`, body) : api.post('/api/veterinarios', body);
  }, onSaved);
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <Field label="Nombre completo" error={fe.nombre}><input {...bind('nombre')} /></Field>
      <div className="grid2">
        <Field label="Documento" error={fe.documento}><input {...bind('documento')} /></Field>
        <Field label="Teléfono" error={fe.telefono} hint="10 dígitos"><input {...bind('telefono')} inputMode="numeric" /></Field>
      </div>
      <div className="grid2">
        <Field label="Correo electrónico" error={fe.email}><input type="email" {...bind('email')} /></Field>
        <Field label="Especialidad" error={fe.especialidad}><input {...bind('especialidad')} /></Field>
      </div>
      {vet && <label className="check"><input type="checkbox" checked={v.activo} onChange={(e) => set('activo', e.target.checked)} /> Veterinario activo</label>}
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar veterinario'}</button>
      </div>
    </form>
  );
}

export default function Veterinarios() {
  const [modal, setModal] = useState(null);
  const [cred, setCred] = useState(null); // veterinario recién creado (con su clave temporal)
  const [aviso, avisoEl] = useAviso();
  const { data, loading, error, reload } = useLoad(() => api.get('/api/veterinarios'));
  return (
    <>
      <Titulo accion={<button className="primary" onClick={() => setModal('nuevo')}>Nuevo veterinario</button>}>Veterinarios</Titulo>
      {avisoEl}
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>Aún no hay veterinarios. Crea el primero para poder programar citas.</Vacio>}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Nombre</th><th>Documento</th><th>Teléfono</th><th>Especialidad</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {data.map((x) => (
                <tr key={x.id}>
                  <td><strong>{x.nombre}</strong><br /><span className="muted">{x.email || ''}</span></td>
                  <td>{x.documento}</td><td>{x.telefono}</td><td>{x.especialidad || '—'}</td>
                  <td><span className={`badge ${x.activo ? 'ATENDIDA' : 'CANCELADA'}`}>{x.activo ? 'Activo' : 'Inactivo'}</span></td>
                  <td className="right"><button onClick={() => setModal(x)}>Editar</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal && (
        <Modal title={modal === 'nuevo' ? 'Nuevo veterinario' : 'Editar veterinario'} onClose={() => setModal(null)}>
          <VetForm vet={modal === 'nuevo' ? null : modal} onCancel={() => setModal(null)} onSaved={(r) => { const nuevo = modal === 'nuevo'; setModal(null); reload(); if (nuevo) setCred(r); else aviso('Veterinario guardado.'); }} />
        </Modal>
      )}
      {cred && (
        <Modal title="Veterinario registrado" onClose={() => setCred(null)}>
          <CredencialesVet v={cred} onClose={() => setCred(null)} />
        </Modal>
      )}
    </>
  );
}

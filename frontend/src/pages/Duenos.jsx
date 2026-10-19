import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Modal, Titulo, Vacio, useAviso, useSubmit } from '../ui';

// Al crear un dueño, el backend le abre una cuenta y devuelve su clave temporal UNA sola vez.
export function CredencialesDueno({ d, onClose }) {
  const [copiado, setCopiado] = useState(false);
  if (!d.passwordTemporal) {
    return (
      <>
        <p>{d.nombre} quedó registrado.</p>
        <div className="actions"><button className="primary" onClick={onClose}>Entendido</button></div>
      </>
    );
  }
  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(`Usuario: ${d.usuario}\nContraseña temporal: ${d.passwordTemporal}`);
      setCopiado(true);
    } catch { /* el usuario puede copiarla a mano */ }
  };
  return (
    <div className="cred">
      <p><strong>{d.nombre}</strong> quedó registrado. Entrégale estos datos de acceso:</p>
      <dl>
        <dt>Usuario</dt><dd><code>{d.usuario}</code></dd>
        <dt>Contraseña temporal</dt><dd><code>{d.passwordTemporal}</code></dd>
      </dl>
      <div className="alert error">Anótala ahora: no se volverá a mostrar. El dueño puede cambiarla en «Mi cuenta».</div>
      <div className="actions">
        <button onClick={copiar}>{copiado ? 'Copiado ✓' : 'Copiar'}</button>
        <button className="primary" onClick={onClose}>Entendido</button>
      </div>
    </div>
  );
}

// Formulario reutilizable: también se usa dentro de "Nueva mascota" y en "Mi cuenta".
export function DuenoForm({ dueno, onSaved, onCancel }) {
  const { v, bind } = useForm({
    nombre: dueno?.nombre || '',
    documento: dueno?.documento || '',
    telefono: dueno?.telefono || '',
    email: dueno?.email || '',
    direccion: dueno?.direccion || '',
  });
  const { busy, error, enviar } = useSubmit(
    () => {
      const body = { ...v, email: v.email.trim() || null };
      return dueno ? api.put(`/api/duenos/${dueno.id}`, body) : api.post('/api/duenos', body);
    },
    onSaved
  );
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <Field label="Nombre completo" error={fe.nombre}><input {...bind('nombre')} /></Field>
      <div className="grid2">
        <Field label="Documento" error={fe.documento}><input {...bind('documento')} /></Field>
        <Field label="Teléfono" error={fe.telefono} hint="10 dígitos"><input {...bind('telefono')} inputMode="numeric" /></Field>
      </div>
      <Field label="Correo electrónico" error={fe.email} hint="Aquí recibe confirmaciones de citas y recordatorios de vacunas"><input type="email" {...bind('email')} /></Field>
      <Field label="Dirección" error={fe.direccion}><input {...bind('direccion')} /></Field>
      <ErrorBox error={error} />
      <div className="actions">
        {onCancel && <button type="button" onClick={onCancel}>Cancelar</button>}
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar dueño'}</button>
      </div>
    </form>
  );
}

export default function Duenos() {
  const { can } = useAuth();
  const [q, setQ] = useState('');
  const [modal, setModal] = useState(null); // null | 'nuevo' | dueño a editar
  const [cred, setCred] = useState(null);   // dueño recién creado (con su clave temporal)
  const [aviso, avisoEl] = useAviso();
  const { data, loading, error, reload } = useLoad(
    () => api.get('/api/duenos', { nombre: q.trim() }),
    [q]
  );

  return (
    <>
      <Titulo accion={can.clientes && <button className="primary" onClick={() => setModal('nuevo')}>Nuevo dueño</button>}>Dueños</Titulo>
      {avisoEl}
      <input className="search" placeholder="Buscar por nombre…" value={q} onChange={(e) => setQ(e.target.value)} />
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>No hay dueños{q ? ' con ese nombre' : ' registrados todavía'}.</Vacio>}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Nombre</th><th>Documento</th><th>Teléfono</th><th>Correo</th><th>Dirección</th><th /></tr></thead>
            <tbody>
              {data.map((d) => (
                <tr key={d.id}>
                  <td>{d.nombre}</td><td>{d.documento}</td><td>{d.telefono}</td><td>{d.email || '—'}</td><td>{d.direccion}</td>
                  <td className="right">{can.clientes && <button onClick={() => setModal(d)}>Editar</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal && (
        <Modal title={modal === 'nuevo' ? 'Nuevo dueño' : 'Editar dueño'} onClose={() => setModal(null)}>
          <DuenoForm
            dueno={modal === 'nuevo' ? null : modal}
            onCancel={() => setModal(null)}
            onSaved={(r) => {
              const nuevo = modal === 'nuevo';
              setModal(null);
              reload();
              if (nuevo) setCred(r); else aviso('Dueño guardado.');
            }}
          />
        </Modal>
      )}
      {cred && (
        <Modal title="Dueño registrado" onClose={() => setCred(null)}>
          <CredencialesDueno d={cred} onClose={() => setCred(null)} />
        </Modal>
      )}
    </>
  );
}

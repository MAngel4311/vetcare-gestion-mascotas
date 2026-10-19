import { useState } from 'react';
import { api, fmtFecha, fmtFechaHora, MOTIVOS, hoy } from '../api';
import { useAuth } from '../auth';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Modal, Titulo, Vacio, useAviso, useSubmit } from '../ui';
import { CredencialesDueno, DuenoForm } from './Duenos';

const ESPECIES = ['Perro', 'Gato', 'Ave', 'Conejo', 'Otro'];

function MascotaForm({ mascota, onSaved, onCancel }) {
  const { user, can } = useAuth();
  // El dueño no puede listar dueños: sus mascotas siempre quedan a su nombre.
  const { data: duenos, reload } = useLoad(() => (can.dueno ? Promise.resolve([]) : api.get('/api/duenos')));
  const [nuevoDueno, setNuevoDueno] = useState(false);
  const [cred, setCred] = useState(null);
  const { v, set, bind } = useForm({
    nombre: mascota?.nombre || '',
    especie: mascota?.especie || '',
    raza: mascota?.raza || '',
    fechaNacimiento: mascota?.fechaNacimiento || '',
    pesoKg: mascota?.pesoKg ?? '',
    duenoId: can.dueno ? user.duenoId : mascota?.duenoId || '',
    activo: mascota ? mascota.activo : true,
  });
  const { busy, error, enviar } = useSubmit(() => {
    const body = {
      nombre: v.nombre,
      especie: v.especie,
      raza: v.raza.trim() || null,
      pesoKg: v.pesoKg === '' ? null : Number(v.pesoKg),
      duenoId: v.duenoId === '' ? null : Number(v.duenoId),
      fechaNacimiento: v.fechaNacimiento || null,
      ...(mascota ? { activo: v.activo } : {}),
    };
    return mascota ? api.put(`/api/mascotas/${mascota.id}`, body) : api.post('/api/mascotas', body);
  }, onSaved);
  const fe = error?.fieldErrors || {};

  // Caso "dueño nuevo": se registra aquí mismo y queda seleccionado.
  if (nuevoDueno) {
    return (
      <>
        <p className="muted">Registra al dueño y volverás al formulario de la mascota.</p>
        <DuenoForm
          onCancel={() => setNuevoDueno(false)}
          onSaved={(d) => { reload(); set('duenoId', d.id); setNuevoDueno(false); setCred(d); }}
        />
      </>
    );
  }
  // Clave temporal del dueño recién creado: se muestra una sola vez.
  if (cred) {
    return <CredencialesDueno d={cred} onClose={() => setCred(null)} />;
  }
  return (
    <form onSubmit={enviar} className="form">
      <div className="grid2">
        <Field label="Nombre" error={fe.nombre}><input {...bind('nombre')} /></Field>
        <Field label="Especie" error={fe.especie}>
          <select {...bind('especie')}>
            <option value="">Selecciona…</option>
            {ESPECIES.map((e) => <option key={e}>{e}</option>)}
          </select>
        </Field>
      </div>
      <div className="grid2">
        <Field label="Raza" error={fe.raza}><input {...bind('raza')} /></Field>
        <Field label="Fecha de nacimiento" error={fe.fechaNacimiento}><input type="date" max={hoy()} {...bind('fechaNacimiento')} /></Field>
      </div>
      <Field label="Peso (kg)" error={fe.pesoKg}><input type="number" step="0.01" min="0" {...bind('pesoKg')} /></Field>
      {!can.dueno && (
        <Field label="Dueño" error={fe.duenoId}>
          <div className="row">
            <select {...bind('duenoId')}>
              <option value="">Selecciona un dueño…</option>
              {(duenos || []).map((d) => <option key={d.id} value={d.id}>{d.nombre} — {d.documento}</option>)}
            </select>
            <button type="button" onClick={() => setNuevoDueno(true)}>Nuevo dueño</button>
          </div>
        </Field>
      )}
      {mascota && (
        <label className="check">
          <input type="checkbox" checked={v.activo} onChange={(e) => set('activo', e.target.checked)} />
          Mascota activa <span className="muted">(si se da de baja conserva su historial, pero no admite citas nuevas)</span>
        </label>
      )}
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar mascota'}</button>
      </div>
    </form>
  );
}

function Historial({ mascota }) {
  const { data, loading, error } = useLoad(() => api.get(`/api/mascotas/${mascota.id}/historial`));
  return (
    <>
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>{mascota.nombre} aún no tiene consultas registradas.</Vacio>}
      {data?.map((r) => (
        <article key={r.id} className="record">
          <header>
            <strong>{fmtFechaHora(r.fechaConsulta)}</strong>
            <span className="muted">{MOTIVOS[r.motivo]} · {r.veterinarioNombre}</span>
          </header>
          <dl>
            <dt>Diagnóstico</dt><dd>{r.diagnostico}</dd>
            <dt>Tratamiento</dt><dd>{r.tratamiento}</dd>
            {r.observaciones && <><dt>Observaciones</dt><dd>{r.observaciones}</dd></>}
            {(r.pesoKg || r.temperaturaC || r.alergias) && (
              <>
                <dt>Examen</dt>
                <dd>
                  {r.pesoKg && `Peso ${r.pesoKg} kg`}{r.pesoKg && r.temperaturaC && ' · '}
                  {r.temperaturaC && `Temperatura ${r.temperaturaC} °C`}
                  {r.alergias && ` · Alergias: ${r.alergias}`}
                </dd>
              </>
            )}
          </dl>
        </article>
      ))}
    </>
  );
}

function Vacunas({ mascota }) {
  const { can } = useAuth();
  const { data, loading, error, reload } = useLoad(() => api.get(`/api/mascotas/${mascota.id}/vacunas`));
  const { v, bind } = useForm({ nombreVacuna: '', fechaAplicacion: hoy(), proximaFecha: '', observaciones: '' });
  const { busy, error: errForm, enviar } = useSubmit(
    () => api.post(`/api/mascotas/${mascota.id}/vacunas`, {
      ...v,
      proximaFecha: v.proximaFecha || null,
      observaciones: v.observaciones.trim() || null,
    }),
    reload
  );
  const fe = errForm?.fieldErrors || {};
  return (
    <>
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>Sin vacunas registradas.</Vacio>}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Vacuna</th><th>Aplicada</th><th>Próxima dosis</th><th>Observaciones</th></tr></thead>
            <tbody>
              {data.map((x) => (
                <tr key={x.id}><td>{x.nombreVacuna}</td><td>{fmtFecha(x.fechaAplicacion)}</td><td>{fmtFecha(x.proximaFecha)}</td><td>{x.observaciones || '—'}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {can.clinico && (
        <form onSubmit={enviar} className="form sub">
          <h3>Registrar vacuna</h3>
          <div className="grid2">
            <Field label="Vacuna" error={fe.nombreVacuna}><input {...bind('nombreVacuna')} /></Field>
            <Field label="Fecha de aplicación" error={fe.fechaAplicacion}><input type="date" max={hoy()} {...bind('fechaAplicacion')} /></Field>
          </div>
          <Field label="Próxima dosis" error={fe.proximaFecha} hint="Opcional. Debe ser posterior a la aplicación."><input type="date" {...bind('proximaFecha')} /></Field>
          <Field label="Observaciones" error={fe.observaciones}><input {...bind('observaciones')} /></Field>
          <ErrorBox error={errForm} />
          <div className="actions"><button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Registrar vacuna'}</button></div>
        </form>
      )}
    </>
  );
}

export default function Mascotas() {
  const { can } = useAuth();
  const [q, setQ] = useState('');
  const [especie, setEspecie] = useState('');
  const [modal, setModal] = useState(null); // { tipo: 'form'|'historial'|'vacunas', mascota? }
  const [aviso, avisoEl] = useAviso();
  const { data, loading, error, reload } = useLoad(
    () => api.get('/api/mascotas', { nombre: q.trim(), especie }), [q, especie]
  );

  return (
    <>
      <Titulo accion={can.mascotas && <button className="primary" onClick={() => setModal({ tipo: 'form' })}>Nueva mascota</button>}>
        {can.dueno ? 'Mis mascotas' : 'Mascotas'}
      </Titulo>
      {avisoEl}
      <div className="filters">
        <Field label="Buscar por nombre"><input placeholder="Nombre…" value={q} onChange={(e) => setQ(e.target.value)} /></Field>
        <Field label="Especie">
          <select value={especie} onChange={(e) => setEspecie(e.target.value)}>
            <option value="">Todas</option>
            {ESPECIES.map((e) => <option key={e}>{e}</option>)}
          </select>
        </Field>
      </div>
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>No hay mascotas{q || especie ? ' con esos filtros' : ' registradas todavía'}.</Vacio>}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Nombre</th><th>Especie</th><th>Raza</th><th>Nacimiento</th><th>Peso</th>{!can.dueno && <th>Dueño</th>}<th /></tr></thead>
            <tbody>
              {data.map((m) => (
                <tr key={m.id} className={m.activo ? '' : 'baja'}>
                  <td><strong>{m.nombre}</strong>{!m.activo && <> <span className="badge CANCELADA">De baja</span></>}</td>
                  <td>{m.especie}</td><td>{m.raza || '—'}</td>
                  <td>{fmtFecha(m.fechaNacimiento)}</td><td>{m.pesoKg} kg</td>
                  {!can.dueno && <td>{m.duenoNombre}</td>}
                  <td className="right nowrap">
                    {(can.clinico || can.dueno) && <button onClick={() => setModal({ tipo: 'historial', mascota: m })}>Historial</button>}
                    <button onClick={() => setModal({ tipo: 'vacunas', mascota: m })}>Vacunas</button>
                    {can.mascotas && <button onClick={() => setModal({ tipo: 'form', mascota: m })}>Editar</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {modal?.tipo === 'form' && (
        <Modal title={modal.mascota ? 'Editar mascota' : 'Nueva mascota'} onClose={() => setModal(null)}>
          <MascotaForm mascota={modal.mascota} onCancel={() => setModal(null)} onSaved={() => { setModal(null); aviso('Mascota guardada.'); reload(); }} />
        </Modal>
      )}
      {modal?.tipo === 'historial' && (
        <Modal wide title={`Historial clínico de ${modal.mascota.nombre}`} onClose={() => setModal(null)}><Historial mascota={modal.mascota} /></Modal>
      )}
      {modal?.tipo === 'vacunas' && (
        <Modal wide title={`Vacunas de ${modal.mascota.nombre}`} onClose={() => setModal(null)}><Vacunas mascota={modal.mascota} /></Modal>
      )}
    </>
  );
}

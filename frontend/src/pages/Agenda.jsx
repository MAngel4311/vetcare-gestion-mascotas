import { useEffect, useState } from 'react';
import { api, duracion, fmtFecha, hoy, hora, sumarMinutos, MOTIVOS, ESTADOS } from '../api';
import { useAuth } from '../auth';
import { useForm, useLoad } from '../hooks';
import { Cargando, ErrorBox, Estado, Field, Modal, Titulo, Vacio, useAviso, useSubmit } from '../ui';

const DURACIONES = [15, 30, 45, 60, 90];

// Muestra los huecos libres del veterinario y deja elegir uno (apoyo visual; el backend valida el choque).
function Huecos({ veterinarioId, fecha, duracionMin, alElegir }) {
  const [disp, setDisp] = useState(null);
  useEffect(() => {
    if (!veterinarioId || !fecha) { setDisp(null); return undefined; }
    let vivo = true;
    api.get(`/api/veterinarios/${veterinarioId}/disponibilidad`, { fecha, duracionMinutos: duracionMin })
      .then((d) => vivo && setDisp(d)).catch(() => vivo && setDisp(null));
    return () => { vivo = false; };
  }, [veterinarioId, fecha, duracionMin]);
  if (!disp) return null;
  return (
    <div className="huecos">
      <span className="muted">Horas libres ese día:</span>
      {disp.libres.length === 0 && <strong> sin espacio para {duracionMin} min</strong>}
      <div className="chips">
        {disp.libres.map((r) => (
          <button type="button" key={r.inicio} className="chip" onClick={() => alElegir(hora(r.inicio))}>
            {hora(r.inicio)}–{hora(r.fin)}
          </button>
        ))}
      </div>
      {disp.ocupados.length > 0 && (
        <small className="muted">Ocupado: {disp.ocupados.map((o) => `${hora(o.inicio)}–${hora(o.fin)} (${o.mascota})`).join(', ')}</small>
      )}
    </div>
  );
}

function NuevaCita({ onSaved, onCancel }) {
  const { data: mascotas } = useLoad(() => api.get('/api/mascotas'));
  const { data: vets } = useLoad(() => api.get('/api/veterinarios', { activo: true }));
  const { v, set, bind } = useForm({ mascotaId: '', veterinarioId: '', fecha: hoy(), hora: '09:00', duracion: 30, motivo: 'CONTROL' });
  const { busy, error, enviar } = useSubmit(() => api.post('/api/citas', {
    mascotaId: v.mascotaId === '' ? null : Number(v.mascotaId),
    veterinarioId: v.veterinarioId === '' ? null : Number(v.veterinarioId),
    fechaHoraInicio: `${v.fecha}T${v.hora}:00`,
    fechaHoraFin: sumarMinutos(v.fecha, v.hora, v.duracion),
    motivo: v.motivo,
  }), onSaved);
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <Field label="Mascota" error={fe.mascotaId} hint="Solo aparecen las mascotas activas">
        <select {...bind('mascotaId')}>
          <option value="">Selecciona una mascota…</option>
          {(mascotas || []).filter((m) => m.activo).map((m) => <option key={m.id} value={m.id}>{m.nombre} — {m.duenoNombre}</option>)}
        </select>
      </Field>
      <Field label="Veterinario" error={fe.veterinarioId}>
        <select {...bind('veterinarioId')}>
          <option value="">Selecciona un veterinario…</option>
          {(vets || []).map((x) => <option key={x.id} value={x.id}>{x.nombre}{x.especialidad ? ` — ${x.especialidad}` : ''}</option>)}
        </select>
      </Field>
      <div className="grid3">
        <Field label="Fecha" hint="Lunes a sábado"><input type="date" min={hoy()} {...bind('fecha')} /></Field>
        <Field label="Hora" error={fe.fechaHoraInicio} hint="8:00 a 18:00"><input type="time" min="08:00" max="18:00" step="900" {...bind('hora')} /></Field>
        <Field label="Duración">
          <select value={v.duracion} onChange={(e) => set('duracion', Number(e.target.value))}>
            {DURACIONES.map((d) => <option key={d} value={d}>{d} min</option>)}
          </select>
        </Field>
      </div>
      <Huecos veterinarioId={v.veterinarioId} fecha={v.fecha} duracionMin={v.duracion} alElegir={(h) => set('hora', h)} />
      <Field label="Motivo" error={fe.motivo}>
        <select {...bind('motivo')}>{Object.entries(MOTIVOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}</select>
      </Field>
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Programando…' : 'Programar cita'}</button>
      </div>
    </form>
  );
}

function Reprogramar({ cita, onSaved, onCancel }) {
  const { v, set, bind } = useForm({ fecha: cita.fechaHoraInicio.slice(0, 10), hora: hora(cita.fechaHoraInicio), duracion: duracion(cita.fechaHoraInicio, cita.fechaHoraFin) });
  const { busy, error, enviar } = useSubmit(() => api.put(`/api/citas/${cita.id}/reprogramar`, {
    fechaHoraInicio: `${v.fecha}T${v.hora}:00`,
    fechaHoraFin: sumarMinutos(v.fecha, v.hora, v.duracion),
  }), onSaved);
  return (
    <form onSubmit={enviar} className="form">
      <p>{cita.mascotaNombre} con {cita.veterinarioNombre}</p>
      <div className="grid2">
        <Field label="Nueva fecha"><input type="date" min={hoy()} {...bind('fecha')} /></Field>
        <Field label="Nueva hora"><input type="time" min="08:00" max="18:00" step="900" {...bind('hora')} /></Field>
      </div>
      <Huecos veterinarioId={cita.veterinarioId} fecha={v.fecha} duracionMin={v.duracion} alElegir={(h) => set('hora', h)} />
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Reprogramar'}</button>
      </div>
    </form>
  );
}

// RF-09: toda cancelación lleva un motivo.
function CancelarCita({ cita, onSaved, onCancel }) {
  const { v, bind } = useForm({ motivo: '' });
  const { busy, error, enviar } = useSubmit(() => api.patch(`/api/citas/${cita.id}/cancelar`, { motivo: v.motivo.trim() }), onSaved);
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <p>Vas a cancelar la cita de <strong>{cita.mascotaNombre}</strong> el {fmtFecha(cita.fechaHoraInicio)} a las {hora(cita.fechaHoraInicio)}. El horario quedará libre y el dueño recibirá un aviso por correo.</p>
      <Field label="Motivo de la cancelación" error={fe.motivo}><textarea rows="3" maxLength="255" {...bind('motivo')} autoFocus /></Field>
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Volver</button>
        <button className="danger solid" disabled={busy}>{busy ? 'Cancelando…' : 'Cancelar cita'}</button>
      </div>
    </form>
  );
}

function RegistrarConsulta({ cita, onSaved, onCancel }) {
  const { v, bind } = useForm({ diagnostico: '', tratamiento: '', observaciones: '', pesoKg: '', temperaturaC: '', alergias: '' });
  const num = (x) => (x === '' ? null : Number(x));
  const { busy, error, enviar } = useSubmit(() => api.post(`/api/citas/${cita.id}/registro-clinico`, {
    diagnostico: v.diagnostico,
    tratamiento: v.tratamiento,
    observaciones: v.observaciones.trim() || null,
    pesoKg: num(v.pesoKg),
    temperaturaC: num(v.temperaturaC),
    alergias: v.alergias.trim() || null,
  }), onSaved);
  const fe = error?.fieldErrors || {};
  return (
    <form onSubmit={enviar} className="form">
      <p>{cita.mascotaNombre} · {cita.duenoNombre} · {MOTIVOS[cita.motivo]}</p>
      <Field label="Diagnóstico" error={fe.diagnostico}><textarea rows="3" {...bind('diagnostico')} /></Field>
      <Field label="Tratamiento" error={fe.tratamiento}><textarea rows="3" {...bind('tratamiento')} /></Field>
      <Field label="Observaciones" error={fe.observaciones}><textarea rows="2" {...bind('observaciones')} /></Field>
      <div className="grid3">
        <Field label="Peso (kg)" error={fe.pesoKg}><input type="number" step="0.01" {...bind('pesoKg')} /></Field>
        <Field label="Temperatura (°C)" error={fe.temperaturaC}><input type="number" step="0.1" {...bind('temperaturaC')} /></Field>
        <Field label="Alergias" error={fe.alergias}><input {...bind('alergias')} /></Field>
      </div>
      <ErrorBox error={error} />
      <div className="actions">
        <button type="button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar consulta'}</button>
      </div>
    </form>
  );
}

export default function Agenda() {
  const { can } = useAuth();
  // El personal ve por defecto el día de hoy; el dueño ve todas sus citas.
  const [fecha, setFecha] = useState(can.dueno ? '' : hoy());
  const [vetId, setVetId] = useState('');
  const [estado, setEstado] = useState('');
  const [modal, setModal] = useState(null); // { tipo, cita? }
  const [aviso, avisoEl] = useAviso();
  const [errAccion, setErrAccion] = useState(null);
  // El dueño no tiene permiso para listar veterinarios (el backend responde 403).
  const { data: vets } = useLoad(() => (can.dueno ? Promise.resolve([]) : api.get('/api/veterinarios')));
  const { data, loading, error, reload } = useLoad(
    () => api.get('/api/citas', { fecha, veterinarioId: vetId, estado }), [fecha, vetId, estado]
  );
  const citas = [...(data || [])].sort((a, b) => a.fechaHoraInicio.localeCompare(b.fechaHoraInicio));
  const cerrar = (msg) => { setModal(null); if (msg) aviso(msg); reload(); };

  const confirmar = async (c) => {
    setErrAccion(null);
    try { await api.patch(`/api/citas/${c.id}/confirmar`); cerrar('Cita confirmada. El dueño recibió el aviso.'); } catch (e) { setErrAccion(e); }
  };

  return (
    <>
      <Titulo accion={can.clientes && <button className="primary" onClick={() => setModal({ tipo: 'nueva' })}>Nueva cita</button>}>
        {can.dueno ? 'Mis citas' : 'Agenda de citas'}
      </Titulo>
      {avisoEl}
      <div className="filters">
        <Field label="Fecha"><input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} /></Field>
        {!can.dueno && (
          <Field label="Veterinario">
            <select value={vetId} onChange={(e) => setVetId(e.target.value)}>
              <option value="">Todos</option>
              {(vets || []).map((x) => <option key={x.id} value={x.id}>{x.nombre}</option>)}
            </select>
          </Field>
        )}
        <Field label="Estado">
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ESTADOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
          </select>
        </Field>
        <button onClick={() => setFecha(hoy())}>Hoy</button>
        <button onClick={() => setFecha('')}>Todas las fechas</button>
      </div>
      {loading && <Cargando />}
      <ErrorBox error={error || errAccion} />
      {data && citas.length === 0 && <Vacio>No hay citas {fecha ? 'para este día' : ''} con esos filtros.</Vacio>}
      <ol className="timeline">
        {citas.map((c) => {
          const vigente = c.estado === 'PROGRAMADA' || c.estado === 'CONFIRMADA';
          return (
            <li key={c.id} className={`cita ${c.estado}`}>
              <div className="hora">
                <strong>{hora(c.fechaHoraInicio)}</strong>
                <span>{hora(c.fechaHoraFin)}</span>
                {!fecha && <span>{fmtFecha(c.fechaHoraInicio)}</span>}
              </div>
              <div className="info">
                <div className="top"><strong>{c.mascotaNombre}</strong><Estado estado={c.estado} /></div>
                <span className="muted">{c.duenoNombre} · {MOTIVOS[c.motivo]} · {c.veterinarioNombre}</span>
                {c.estado === 'CANCELADA' && c.motivoCancelacion && <span className="muted">Motivo de cancelación: {c.motivoCancelacion}</span>}
              </div>
              {!can.dueno && vigente && (
                <div className="acciones">
                  {can.clientes && c.estado === 'PROGRAMADA' && <button className="primary" onClick={() => confirmar(c)}>Confirmar</button>}
                  {can.clinico && c.estado === 'CONFIRMADA' && <button className="primary" onClick={() => setModal({ tipo: 'consulta', cita: c })}>Registrar consulta</button>}
                  {can.clinico && !can.clientes && c.estado === 'PROGRAMADA' && <span className="muted">Pendiente de confirmar por recepción</span>}
                  {can.clientes && <button onClick={() => setModal({ tipo: 'reprogramar', cita: c })}>Reprogramar</button>}
                  {can.clientes && <button className="danger" onClick={() => setModal({ tipo: 'cancelar', cita: c })}>Cancelar</button>}
                </div>
              )}
            </li>
          );
        })}
      </ol>
      {modal?.tipo === 'nueva' && <Modal title="Nueva cita" onClose={() => setModal(null)}><NuevaCita onCancel={() => setModal(null)} onSaved={() => cerrar('Cita programada.')} /></Modal>}
      {modal?.tipo === 'reprogramar' && <Modal title="Reprogramar cita" onClose={() => setModal(null)}><Reprogramar cita={modal.cita} onCancel={() => setModal(null)} onSaved={() => cerrar('Cita reprogramada.')} /></Modal>}
      {modal?.tipo === 'cancelar' && <Modal title="Cancelar cita" onClose={() => setModal(null)}><CancelarCita cita={modal.cita} onCancel={() => setModal(null)} onSaved={() => cerrar('Cita cancelada. El horario quedó libre.')} /></Modal>}
      {modal?.tipo === 'consulta' && <Modal wide title="Registrar consulta" onClose={() => setModal(null)}><RegistrarConsulta cita={modal.cita} onCancel={() => setModal(null)} onSaved={() => cerrar('Consulta registrada. La cita quedó atendida.')} /></Modal>}
    </>
  );
}

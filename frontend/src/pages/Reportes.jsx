import { useState } from 'react';
import { api, fmtFechaHora, hoy, MOTIVOS, ESTADOS } from '../api';
import { useLoad } from '../hooks';
import { Cargando, ErrorBox, Estado, Field, Titulo, Vacio } from '../ui';

const primerDiaMes = () => `${hoy().slice(0, 8)}01`;

export default function Reportes() {
  const [desde, setDesde] = useState(primerDiaMes());
  const [hasta, setHasta] = useState(hoy());
  const [vetId, setVetId] = useState('');
  const [estado, setEstado] = useState('');
  const [errCsv, setErrCsv] = useState(null);
  const { data: vets } = useLoad(() => api.get('/api/veterinarios'));
  const params = { desde, hasta, veterinarioId: vetId, estado };
  const lista = useLoad(() => api.get('/api/reportes/consultas', params), [desde, hasta, vetId, estado]);
  const resumen = useLoad(() => api.get('/api/reportes/resumen', params), [desde, hasta, vetId, estado]);
  const bajarCsv = async () => {
    setErrCsv(null);
    try { await api.download('/api/reportes/consultas/csv', params, `reporte-consultas_${desde}_${hasta}.csv`); } catch (e) { setErrCsv(e); }
  };
  const maxMotivo = Math.max(1, ...(resumen.data?.porMotivo || []).map((m) => m.total));
  const maxVet = Math.max(1, ...(resumen.data?.porVeterinario || []).map((m) => m.total));

  return (
    <>
      <Titulo accion={lista.data?.length > 0 && (
        <span className="row">
          <button onClick={bajarCsv}>Descargar CSV (Excel)</button>
          <button onClick={() => window.print()}>Imprimir / PDF</button>
        </span>
      )}>Reporte de consultas</Titulo>
      <div className="filters">
        <Field label="Desde"><input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></Field>
        <Field label="Hasta"><input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></Field>
        <Field label="Veterinario">
          <select value={vetId} onChange={(e) => setVetId(e.target.value)}>
            <option value="">Todos</option>
            {(vets || []).map((x) => <option key={x.id} value={x.id}>{x.nombre}</option>)}
          </select>
        </Field>
        <Field label="Estado">
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Atendidas</option>
            {Object.entries(ESTADOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
          </select>
        </Field>
      </div>
      <ErrorBox error={lista.error || resumen.error || errCsv} />
      {resumen.data && (
        <div className="summary">
          <div className="total"><strong>{resumen.data.totalConsultas}</strong><span>consultas en el periodo</span></div>
          <div>
            <h3>Por veterinario</h3>
            {resumen.data.porVeterinario.map((x) => (
              <div className="bar" key={x.veterinarioId}><span>{x.veterinario}</span><i style={{ width: `${(x.total / maxVet) * 100}%` }} /><b>{x.total}</b></div>
            ))}
          </div>
          <div>
            <h3>Por motivo</h3>
            {resumen.data.porMotivo.map((x) => (
              <div className="bar" key={x.motivo}><span>{MOTIVOS[x.motivo]}</span><i style={{ width: `${(x.total / maxMotivo) * 100}%` }} /><b>{x.total}</b></div>
            ))}
          </div>
        </div>
      )}
      {lista.loading && <Cargando />}
      {lista.data && lista.data.length === 0 && <Vacio>No hay consultas en ese periodo.</Vacio>}
      {lista.data && lista.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Fecha</th><th>Mascota</th><th>Veterinario</th><th>Motivo</th><th>Estado</th></tr></thead>
            <tbody>
              {lista.data.map((r) => (
                <tr key={r.citaId}><td>{fmtFechaHora(r.fecha)}</td><td>{r.mascota}</td><td>{r.veterinario}</td><td>{MOTIVOS[r.motivo]}</td><td><Estado estado={r.estado} /></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

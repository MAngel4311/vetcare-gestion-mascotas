import { useState } from 'react';
import { api, fmtFecha } from '../api';
import { useAuth } from '../auth';
import { useLoad } from '../hooks';
import { Cargando, ErrorBox, Field, Titulo, Vacio } from '../ui';

export default function VacunasProximas() {
  const { can } = useAuth();
  const [envio, setEnvio] = useState(null);
  const [errEnvio, setErrEnvio] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const enviarRecordatorios = async () => {
    setEnviando(true); setErrEnvio(null); setEnvio(null);
    try { setEnvio(await api.post('/api/vacunas/recordatorios')); } catch (e) { setErrEnvio(e); } finally { setEnviando(false); }
  };
  const [dias, setDias] = useState(30);
  const [vencidas, setVencidas] = useState(true);
  const { data, loading, error } = useLoad(
    () => api.get('/api/vacunas/proximas', { dias, incluirVencidas: vencidas }), [dias, vencidas]
  );
  return (
    <>
      <Titulo accion={can.admin && <button onClick={enviarRecordatorios} disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar recordatorios ahora'}</button>}>Vacunas próximas a vencer</Titulo>
        {envio && <div className="alert ok" role="status">Se encontraron {envio.pendientes} vacunas por recordar: {envio.enviados} enviadas, {envio.sinCorreo} sin correo del dueño.</div>}
      <ErrorBox error={errEnvio} />
      <div className="filters">
        <Field label="Ventana">
          <select value={dias} onChange={(e) => setDias(Number(e.target.value))}>
            {[15, 30, 60, 90].map((d) => <option key={d} value={d}>Próximos {d} días</option>)}
          </select>
        </Field>
        <label className="check"><input type="checkbox" checked={vencidas} onChange={(e) => setVencidas(e.target.checked)} /> Incluir atrasadas</label>
      </div>
      {loading && <Cargando />}
      <ErrorBox error={error} />
      {data && data.length === 0 && <Vacio>No hay vacunas pendientes en este periodo.</Vacio>}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Mascota</th><th>Vacuna</th><th>Próxima dosis</th><th>Estado</th><th>Contacto del dueño</th></tr></thead>
            <tbody>
              {data.map((x) => (
                <tr key={x.vacunacionId}>
                  <td><strong>{x.mascotaNombre}</strong></td>
                  <td>{x.nombreVacuna}</td>
                  <td>{fmtFecha(x.proximaFecha)}</td>
                  <td>
                    {x.diasRestantes < 0
                      ? <span className="badge CANCELADA">Atrasada {Math.abs(x.diasRestantes)} d</span>
                      : <span className={`badge ${x.diasRestantes <= 7 ? 'PROGRAMADA' : 'ATENDIDA'}`}>En {x.diasRestantes} d</span>}
                  </td>
                  <td>{x.duenoNombre}<br /><span className="muted">{x.duenoTelefono}{x.duenoEmail ? ` · ${x.duenoEmail}` : ''}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

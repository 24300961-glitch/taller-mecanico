import { useState } from 'react';
import { api, fechaCorta, texto } from '../api.js';
import Layout from '../components/Layout.jsx';
import { Alerta, BotonCargando, Campo, Insignia, useAlerta } from '../components/ui.jsx';

const ETIQUETAS = {
  REGISTRO: ['Registro', 'text-bg-success'],
  CAMBIO_TALLER: ['Cambio de taller', 'text-bg-primary'],
  CAMBIO_CLIENTE: ['Cambio de cliente', 'text-bg-info'],
  ACTUALIZACION: ['Actualización', 'text-bg-secondary'],
  DESACTIVACION: ['Desactivación', 'text-bg-warning'],
  REACTIVACION: ['Reactivación', 'text-bg-success'],
};

function agrupar(movs) {
  const grupos = [];
  const indice = {};
  movs.forEach((m) => {
    const k = m.vehiculoId;
    if (!(k in indice)) {
      indice[k] = grupos.length;
      grupos.push({ placa: m.placa, cliente: m.cliente, movs: [] });
    }
    grupos[indice[k]].movs.push(m);
  });
  return grupos;
}

export default function Historial() {
  const [q, setQ] = useState('');
  const [grupos, setGrupos] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const { alerta, mostrarError, limpiar } = useAlerta();

  const buscar = async (ev) => {
    if (ev) ev.preventDefault();
    limpiar();
    setOcupado(true);
    try {
      const v = q.trim();
      const movs = await api(`/api/vehiculos/historial${v ? `?q=${encodeURIComponent(v)}` : ''}`);
      setGrupos(agrupar(movs || []));
    } catch (e) {
      mostrarError(e);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <Layout titulo="Historial de vehículos" subtitulo="Movimientos por cliente, taller y fecha">
      {alerta && <Alerta tipo={alerta.tipo} mensaje={alerta.mensaje} detalles={alerta.detalles} onCerrar={limpiar} />}
      <div className="data-card shadow-sm mb-4">
        <div className="card-header">
          <h2 className="h6 fw-bold mb-0">Buscar movimientos</h2>
        </div>
        <div className="p-3 p-md-4">
          <form className="row g-2" noValidate onSubmit={buscar}>
            <div className="col-md-9">
              <Campo label="Nombre del cliente o placa" htmlFor="qHistorial">
                <input
                  className="form-control" id="qHistorial" name="q" maxLength="120"
                  placeholder="Ej. Ana Torres o ABC-1234" autoComplete="off" value={q}
                  onChange={(e) => setQ(e.target.value)}
                />
              </Campo>
            </div>
            <div className="col-md-3 d-flex align-items-end">
              <BotonCargando type="submit" className="btn btn-accent w-100" cargando={ocupado} textoCargando="Buscando...">
                Buscar
              </BotonCargando>
            </div>
          </form>
          <p className="form-text mt-2 mb-0">Sin texto se muestran los movimientos más recientes.</p>
        </div>
      </div>
      <div>
        {grupos === null && (
          <div className="data-card shadow-sm">
            <p className="empty-state mb-0">Escribe un nombre o presiona Buscar.</p>
          </div>
        )}
        {grupos !== null && grupos.length === 0 && (
          <div className="data-card shadow-sm">
            <p className="empty-state mb-0">No hay movimientos para esa búsqueda.</p>
          </div>
        )}
        {(grupos || []).map((g, i) => (
          <div className="data-card shadow-sm mb-3" key={i}>
            <div className="card-header">
              <h2 className="h6 fw-bold mb-0">
                {g.placa} · {g.cliente}
              </h2>
            </div>
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Movimiento</th>
                    <th>Taller</th>
                    <th>Detalle</th>
                    <th>Por</th>
                  </tr>
                </thead>
                <tbody>
                  {g.movs.map((m) => {
                    const et = ETIQUETAS[m.tipo] || [m.tipo, 'text-bg-secondary'];
                    return (
                      <tr key={m.id}>
                        <td>{fechaCorta(m.fecha)}</td>
                        <td>
                          <Insignia texto={et[0]} clase={et[1]} />
                        </td>
                        <td>{texto(m.taller)}</td>
                        <td>{texto(m.detalle)}</td>
                        <td>{texto(m.por)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </div>
    </Layout>
  );
}

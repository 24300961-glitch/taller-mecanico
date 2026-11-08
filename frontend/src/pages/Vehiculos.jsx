import { useEffect, useRef, useState } from 'react';
import { api, ANIO_MAXIMO, texto } from '../api.js';
import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';
import ComboTaller from '../components/ComboTaller.jsx';
import { Alerta, BotonCargando, Campo, enfocarPrimerError, EstadoBadge, Modal, useAlerta, useToasts } from '../components/ui.jsx';
import { RX, digitos } from '../api.js';

const VACIO_VEH = { tallerId: '', placa: '', marca: '', modelo: '', anio: '', vin: '', color: '', version: '', kilometraje: '', fotos: '' };

function validarVehiculo(d) {
  const e = {};
  if (d.placa && (d.placa.length < 4 || d.placa.length > 14 || !RX.PLACA.test(d.placa)))
    e.placa = 'La placa debe ir en mayúsculas separada por guion (ej. ABC-123)';
  if (d.marca && (d.marca.length < 2 || d.marca.length > 40)) e.marca = 'La marca debe tener entre 2 y 40 caracteres';
  else if (d.marca && !RX.MARCA.test(d.marca)) e.marca = 'La marca contiene caracteres no permitidos';
  if (d.modelo && d.modelo.length > 60) e.modelo = 'El modelo no debe exceder 60 caracteres';
  else if (d.modelo && !RX.MODELO.test(d.modelo)) e.modelo = 'El modelo contiene caracteres no permitidos';
  if (d.anio && (!RX.ANIO.test(d.anio) || Number(d.anio) < 1900 || Number(d.anio) > ANIO_MAXIMO))
    e.anio = `El año debe ser un número de 4 dígitos entre 1900 y ${ANIO_MAXIMO}`;
  if (d.vin && !RX.VIN.test(d.vin)) e.vin = 'El VIN debe tener 17 caracteres alfanuméricos (sin I, O ni Q)';
  if (d.color && !RX.COLOR.test(d.color)) e.color = 'El color solo puede contener letras';
  if (d.version && d.version.length > 30) e.version = 'La versión no debe exceder 30 caracteres';
  if (d.kilometraje && (!/^\d+$/.test(d.kilometraje) || Number(d.kilometraje) < 0)) e.kilometraje = 'El kilometraje debe ser un entero mayor o igual a cero';
  if (d.fotos && d.fotos.length > 500) e.fotos = 'Las rutas de fotos no deben exceder 500 caracteres';
  return e;
}

export default function Vehiculos() {
  const { tienePermiso } = useAuth();
  const P = {
    registrar: tienePermiso('VEHICULOS_REGISTRAR'),
    editar: tienePermiso('VEHICULOS_EDITAR'),
    desactivar: tienePermiso('VEHICULOS_DESACTIVAR'),
  };
  const [q, setQ] = useState('');
  const [inactivos, setInactivos] = useState(false);
  const [lista, setLista] = useState([]);
  const [resumen, setResumen] = useState('');
  const [ocupadoBuscar, setOcupadoBuscar] = useState(false);
  const general = useAlerta();
  const { push, zona } = useToasts();

  const [modal, setModal] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(VACIO_VEH);
  const [errores, setErrores] = useState({});
  const [talleres, setTalleres] = useState([]);
  const [ocupadoGuardar, setOcupadoGuardar] = useState(false);
  const modalM = useAlerta();

  // Autocomplete de propietario
  const [propTexto, setPropTexto] = useState('');
  const [propId, setPropId] = useState('');
  const [sugerencias, setSugerencias] = useState([]);
  const [listaAbierta, setListaAbierta] = useState(false);
  const [resaltado, setResaltado] = useState(-1);
  const [errorProp, setErrorProp] = useState('');
  const timerBusqueda = useRef(null);
  const secuencia = useRef(0);

  const [modalDes, setModalDes] = useState(false);
  const [desactivarId, setDesactivarId] = useState(null);
  const [placaDes, setPlacaDes] = useState('');
  const [motivo, setMotivo] = useState('');
  const [errorMotivo, setErrorMotivo] = useState('');
  const [ocupadoDes, setOcupadoDes] = useState(false);
  const modalDs = useAlerta();

  const buscar = async () => {
    setOcupadoBuscar(true);
    try {
      const v = q.trim();
      const data = await api(`/api/vehiculos?q=${encodeURIComponent(v)}&incluirInactivos=${inactivos}`);
      setLista(data || []);
      setResumen(`${(data || []).length} resultado(s)${(data || []).length >= 50 ? ' (máx 50; refina la búsqueda)' : ''}`);
    } catch (e) {
      general.mostrarError(e);
      setLista([]);
      setResumen('No se pudo cargar la lista.');
    } finally {
      setOcupadoBuscar(false);
    }
  };

  useEffect(() => {
    buscar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const buscarPropietarios = async (textoBusqueda) => {
    const n = ++secuencia.current;
    try {
      const data = await api(`/api/clientes?q=${encodeURIComponent(textoBusqueda)}&incluirInactivos=false`);
      if (secuencia.current !== n) return;
      setSugerencias(data || []);
      setListaAbierta(true);
      setResaltado(-1);
    } catch (e) {
      if (secuencia.current !== n) return;
      setSugerencias([]);
      setListaAbierta(true);
    }
  };

  const programarBusqueda = (textoBusqueda) => {
    if (timerBusqueda.current) clearTimeout(timerBusqueda.current);
    timerBusqueda.current = setTimeout(() => buscarPropietarios(textoBusqueda), 250);
  };

  const alEscribirProp = (v) => {
    setPropTexto(v);
    setPropId('');
    setErrorProp('');
    if (v.length === 1) {
      setListaAbierta(false);
      return;
    }
    programarBusqueda(v);
  };

  const elegirProp = (c) => {
    setPropTexto(`${c.nombreCompleto} (#${c.id})`);
    setPropId(String(c.id));
    setErrorProp('');
    modalM.limpiar();
    setListaAbierta(false);
  };

  const teclaProp = (ev) => {
    if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') {
      ev.preventDefault();
      if (!listaAbierta) return;
      const n = sugerencias.length;
      if (!n) return;
      setResaltado((r) => (ev.key === 'ArrowDown' ? (r + 1) % n : (r - 1 + n) % n));
    } else if (ev.key === 'Enter') {
      if (listaAbierta && resaltado >= 0 && sugerencias[resaltado]) {
        ev.preventDefault();
        elegirProp(sugerencias[resaltado]);
      }
    } else if (ev.key === 'Escape') {
      setListaAbierta(false);
    }
  };

  const cargarTalleres = async (tallerId, nombreExistente) => {
    const sel = [{ id: '', nombre: 'Selecciona un taller' }];
    const data = await api('/api/talleres/activos');
    const ops = [...data];
    if (tallerId && !ops.some((o) => String(o.id) === String(tallerId))) {
      ops.push({ id: tallerId, nombre: `${nombreExistente || 'Taller'} (asignación existente)` });
    }
    setTalleres([{ id: '', nombre: 'Selecciona un taller' }, ...ops]);
    return sel;
  };

  const abrirAlta = async () => {
    setForm(VACIO_VEH);
    setErrores({});
    modalM.limpiar();
    setEditandoId(null);
    setPropTexto('');
    setPropId('');
    setSugerencias([]);
    setListaAbierta(false);
    setErrorProp('');
    try {
      await cargarTalleres(null, null);
    } catch (e) {
      modalM.mostrarError(e);
      return;
    }
    setModal(true);
  };

  const abrirEdicion = async (v) => {
    setForm(VACIO_VEH);
    setErrores({});
    modalM.limpiar();
    setEditandoId(v.id);
    try {
      await cargarTalleres(v.tallerId, v.taller);
    } catch (e) {
      modalM.mostrarError(e);
      return;
    }
    if (v.clienteId && !v.cliente) {
      try {
        const c = await api(`/api/clientes/${v.clienteId}`);
        setPropTexto(`${c.nombreCompleto} (#${c.id})`);
        setPropId(String(c.id));
      } catch (e) {
        setPropTexto('');
        setPropId('');
      }
    } else {
      setPropTexto(v.cliente ? `${v.cliente} (#${v.clienteId})` : '');
      setPropId(v.clienteId ? String(v.clienteId) : '');
    }
    setForm({
      tallerId: v.tallerId || '', placa: v.placa || '', marca: v.marca || '', modelo: v.modelo || '',
      anio: v.anio === null || v.anio === undefined ? '' : String(v.anio), vin: v.vin || '', color: v.color || '',
      version: v.version || '', kilometraje: v.kilometraje === null || v.kilometraje === undefined ? '' : String(v.kilometraje), fotos: v.fotos || '',
    });
    setSugerencias([]);
    setListaAbierta(false);
    setErrorProp('');
    setModal(true);
  };

  const guardar = async () => {
    modalM.limpiar();
    const d = {
      ...form,
      placa: (form.placa || '').toUpperCase(),
      vin: (form.vin || '').toUpperCase(),
      marca: (form.marca || '').trim(),
      modelo: (form.modelo || '').trim(),
      color: (form.color || '').trim(),
    };
    const e = validarVehiculo(d);
    if (!d.placa) e.placa = 'La placa es obligatoria';
    if (!d.marca) e.marca = 'La marca es obligatoria';
    if (!d.modelo) e.modelo = 'El modelo es obligatorio';
    if (!d.anio) e.anio = 'El año es obligatorio';
    if (!d.kilometraje) e.kilometraje = 'El kilometraje es obligatorio';
    if (!d.tallerId) e.tallerId = 'Selecciona un taller activo';
    setErrores(e);
    if (!propId) {
      setErrorProp('Elige un propietario de la lista.');
      modalM.mostrar('danger', 'Selecciona el propietario de la lista de sugerencias.');
      push('Selecciona el propietario de la lista de sugerencias.', false);
      enfocarPrimerError();
      if (Object.keys(e).length === 0) return;
    }
    if (Object.keys(e).length > 0) {
      modalM.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      push('Corrige los campos marcados antes de continuar.', false);
      enfocarPrimerError();
      return;
    }
    setOcupadoGuardar(true);
    try {
      const datos = {
        clienteId: Number(propId), tallerId: Number(d.tallerId), placa: d.placa, marca: d.marca,
        modelo: d.modelo, anio: Number(d.anio), vin: d.vin, color: d.color,
        version: d.version.trim(), kilometraje: d.kilometraje === '' ? null : Number(d.kilometraje), fotos: d.fotos.trim(),
      };
      const url = editandoId ? `/api/vehiculos/${editandoId}` : '/api/vehiculos';
      const r = await api(url, { method: editandoId ? 'PUT' : 'POST', body: datos });
      setModal(false);
      push(r.message, true);
      general.mostrar('success', r.message);
      await buscar();
    } catch (err) {
      if (err.data && err.data.coincidencias) {
        const detalles = err.data.coincidencias.map((c) => `${c.placa} · propietario: ${c.cliente} (#${c.clienteId}) — ${c.motivos.join(', ')}`);
        modalM.mostrar('warning', err.message, detalles);
        const ev = {};
        if (err.data.coincidencias.some((c) => c.motivos.indexOf('Misma placa') !== -1)) ev.placa = 'Placa ya registrada';
        if (err.data.coincidencias.some((c) => c.motivos.indexOf('Mismo VIN') !== -1)) ev.vin = 'VIN ya registrado';
        setErrores(ev);
      } else {
        modalM.mostrarError(err);
        if (err.data && err.data.errores) {
          const porCampo = {};
          Object.keys(err.data.errores).forEach((k) => {
            porCampo[k] = err.data.errores[k];
          });
          setErrores(porCampo);
        }
      }
    } finally {
      setOcupadoGuardar(false);
    }
  };

  const abrirDesactivar = (v) => {
    setDesactivarId(v.id);
    setPlacaDes(v.placa);
    setMotivo('');
    setErrorMotivo('');
    modalDs.limpiar();
    setModalDes(true);
  };

  const enviarDesactivar = async () => {
    modalDs.limpiar();
    const m = motivo.trim();
    if (!m || m.length < 5) {
      setErrorMotivo('El motivo debe tener al menos 5 caracteres.');
      return;
    }
    setOcupadoDes(true);
    try {
      const r = await api(`/api/vehiculos/${desactivarId}/desactivar`, { method: 'PATCH', body: { motivo: m } });
      setModalDes(false);
      push(r.message, true);
      general.mostrar('success', r.message);
      await buscar();
    } catch (e) {
      modalDs.mostrarError(e);
      if (e.data && e.data.errores && e.data.errores.motivo) setErrorMotivo(e.data.errores.motivo);
    } finally {
      setOcupadoDes(false);
    }
  };

  const reactivar = async (id) => {
    try {
      const r = await api(`/api/vehiculos/${id}/reactivar`, { method: 'PATCH' });
      push(r.message, true);
      general.mostrar('success', r.message);
      await buscar();
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const setV = (k) => (e) => {
    let v = e.target.value;
    if (k === 'placa' || k === 'vin') v = v.toUpperCase();
    if (k === 'anio') v = v.replace(/[^0-9]/g, '');
    setForm((f) => ({ ...f, [k]: v }));
  };

  return (
    <Layout titulo="Vehículos" subtitulo="Consulta, registro y estatus por taller y propietario">
      {general.alerta && (
        <Alerta tipo={general.alerta.tipo} mensaje={general.alerta.mensaje} detalles={general.alerta.detalles} onCerrar={general.limpiar} />
      )}
      <div className="data-card shadow-sm mb-4">
        <div className="card-header">
          <form className="row g-2 align-items-center" noValidate onSubmit={(ev) => { ev.preventDefault(); buscar(); }}>
            <div className="col-md">
              <label htmlFor="qVeh" className="visually-hidden">Buscar vehículo</label>
              <input
                type="search" id="qVeh" className="form-control" maxLength="100"
                placeholder="Placa, VIN, marca, modelo, propietario o ID" value={q} onChange={(e) => setQ(e.target.value)}
              />
            </div>
            <div className="col-auto form-check ms-2">
              <input className="form-check-input" type="checkbox" id="incInactivosV" checked={inactivos} onChange={(e) => setInactivos(e.target.checked)} />
              <label className="form-check-label small" htmlFor="incInactivosV">Incluir desactivados</label>
            </div>
            <div className="col-auto">
              <BotonCargando type="submit" className="btn btn-outline-primary" cargando={ocupadoBuscar} textoCargando="Buscando...">
                Buscar
              </BotonCargando>
            </div>
            {P.registrar && (
              <div className="col-auto">
                <button type="button" id="btnNuevoV" className="btn btn-accent" onClick={abrirAlta}>+ Registrar vehículo</button>
              </div>
            )}
          </form>
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>ID</th>
                <th>Placa</th>
                <th>Vehículo</th>
                <th>VIN</th>
                <th>Propietario</th>
                <th>Taller asignado</th>
                <th>Estado</th>
                {(P.editar || P.desactivar) && <th />}
              </tr>
            </thead>
            <tbody id="tablaVeh">
              {lista.length === 0 && (
                <tr>
                  <td colSpan="8" className="empty-state">
                    {resumen ? 'Sin resultados.' : 'Escribe un criterio y presiona Buscar.'}
                  </td>
                </tr>
              )}
              {lista.map((v) => (
                <tr key={v.id}>
                  <td className="small text-muted">#{v.id}</td>
                  <td className="fw-semibold">{v.placa}</td>
                  <td className="small">
                    {[v.marca, v.modelo, v.anio].filter(Boolean).join(' ')}
                    {v.color ? ` · ${v.color}` : ''}
                  </td>
                  <td className="small">{v.vin || '—'}</td>
                  <td className="small">{v.cliente ? `${v.cliente} (#${v.clienteId})` : '—'}</td>
                  <td className="small">{v.taller ? `${v.taller}${v.tallerActivo ? '' : ' (inactivo)'}` : '—'}</td>
                  <td>
                    <EstadoBadge activo={v.activo} textoActivo="Activo" textoInactivo="Desactivado" />
                  </td>
                  {(P.editar || P.desactivar) && (
                    <td>
                      <div className="d-flex gap-1 justify-content-end">
                        {v.activo && P.editar && (
                          <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => abrirEdicion(v)}>
                            Editar
                          </button>
                        )}
                        {v.activo && P.desactivar && (
                          <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => abrirDesactivar(v)}>
                            Desactivar
                          </button>
                        )}
                        {!v.activo && P.desactivar && (
                          <button type="button" className="btn btn-sm btn-outline-success" onClick={() => reactivar(v.id)}>
                            Reactivar
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-3 py-2 small text-muted border-top">{resumen}</div>
      </div>

      <Modal
        abierto={modal}
        grande
        titulo={editandoId ? `Editar vehículo ${form.placa}` : 'Nuevo vehículo'}
        onCerrar={() => setModal(false)}
        onSubmit={guardar}
        pie={
          <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoGuardar} textoCargando="Guardando...">
            {editandoId ? 'Guardar cambios' : 'Guardar vehículo'}
          </BotonCargando>
        }
      >
        {modalM.alerta && (
          <Alerta tipo={modalM.alerta.tipo} mensaje={modalM.alerta.mensaje} detalles={modalM.alerta.detalles} onCerrar={modalM.limpiar} />
        )}
        <div className="mb-3 position-relative">
          <label htmlFor="vhPropietario" className="form-label">Propietario *</label>
          <input
            id="vhPropietario" name="propietario" autoComplete="off" role="combobox" aria-expanded={listaAbierta}
            aria-controls="vhPropietarioLista" placeholder="Escribe al menos 2 letras para buscar"
            className={`form-control${errorProp ? ' is-invalid' : ''}`}
            value={propTexto}
            onChange={(e) => alEscribirProp(e.target.value)}
            onFocus={() => { if (!listaAbierta && propTexto.length !== 1) programarBusqueda(propTexto); }}
            onBlur={() => setTimeout(() => setListaAbierta(false), 120)}
            onKeyDown={teclaProp}
          />
          {errorProp && <div className="invalid-feedback d-block">Elige un propietario de la lista.</div>}
          <div className="form-text">Escribe al menos 2 letras para buscar al propietario.</div>
          {listaAbierta && (
            <ul id="vhPropietarioLista" role="listbox" className="list-group lista-sugerencias shadow">
              {sugerencias.length === 0 && (
                <li className="list-group-item small text-muted">
                  {propTexto ? `Ningún propietario coincide con "${propTexto}".` : 'No hay clientes activos para elegir.'}
                </li>
              )}
              {sugerencias.map((c, i) => (
                <li
                  key={c.id} role="option" aria-selected={i === resaltado}
                  className={`list-group-item list-group-item-action${i === resaltado ? ' active' : ''}`}
                  onMouseDown={(ev) => { ev.preventDefault(); elegirProp(c); }}
                >
                  <div className="fw-semibold">{c.nombreCompleto} (#{c.id})</div>
                  <div className="small text-muted">{c.email ? `${c.email} · ${c.telefono || ''}` : `Cliente #${c.id}`}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
        <ComboTaller opciones={talleres} valor={form.tallerId}
          onChange={(id) => setForm((actual) => ({ ...actual, tallerId: id }))}
          etiqueta="Taller asignado al vehículo *" error={errores.tallerId} />
        <div className="row g-3">
          <div className="col-md-4">
            <Campo label="Placa" required htmlFor="vhPlaca" error={errores.placa}>
              <input id="vhPlaca" name="placa" className={`form-control text-uppercase${errores.placa ? ' is-invalid' : ''}`} required maxLength="14" autoComplete="off" value={form.placa} onChange={setV('placa')} />
              <div className="small text-muted mt-1">Mayúsculas separadas por guion (ej. ABC-123)</div>
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Marca" required htmlFor="vhMarca" error={errores.marca}>
              <input id="vhMarca" name="marca" placeholder="Ej. Nissan" className={`form-control${errores.marca ? ' is-invalid' : ''}`} required maxLength="40" autoComplete="off" value={form.marca} onChange={setV('marca')} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Modelo" required htmlFor="vhModelo" error={errores.modelo}>
              <input id="vhModelo" name="modelo" placeholder="Ej. Versa" className={`form-control${errores.modelo ? ' is-invalid' : ''}`} required maxLength="60" autoComplete="off" value={form.modelo} onChange={setV('modelo')} />
            </Campo>
          </div>
          <div className="col-md-3">
            <Campo label="Año" required htmlFor="vhAnio" error={errores.anio}>
              <input id="vhAnio" name="anio" inputMode="numeric" placeholder="Ej. 2022" className={`form-control${errores.anio ? ' is-invalid' : ''}`} required maxLength="4" value={form.anio} onChange={setV('anio')} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="VIN / Número de serie" htmlFor="vhVin" error={errores.vin}>
              <input id="vhVin" name="vin" className={`form-control text-uppercase${errores.vin ? ' is-invalid' : ''}`} maxLength="17" autoComplete="off" value={form.vin} onChange={setV('vin')} />
            </Campo>
          </div>
          <div className="col-md-3">
            <Campo label="Color" htmlFor="vhColor" error={errores.color}>
              <input id="vhColor" name="color" className={`form-control${errores.color ? ' is-invalid' : ''}`} maxLength="30" value={form.color} onChange={setV('color')} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Versión" htmlFor="vhVersion" error={errores.version}>
              <input id="vhVersion" name="version" className={`form-control${errores.version ? ' is-invalid' : ''}`} maxLength="30" value={form.version} onChange={setV('version')} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Kilometraje" required htmlFor="vhKilometraje" error={errores.kilometraje}>
              <input id="vhKilometraje" name="kilometraje" type="number" min="0" step="1" inputMode="numeric" className={`form-control${errores.kilometraje ? ' is-invalid' : ''}`} required value={form.kilometraje} onChange={setV('kilometraje')} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Rutas de fotos (separadas por coma)" htmlFor="vhFotos" error={errores.fotos}>
              <input id="vhFotos" name="fotos" className={`form-control${errores.fotos ? ' is-invalid' : ''}`} maxLength="500" value={form.fotos} onChange={setV('fotos')} />
            </Campo>
          </div>
        </div>
      </Modal>

      <Modal
        abierto={modalDes}
        titulo="Desactivar vehículo"
        onCerrar={() => setModalDes(false)}
        onSubmit={enviarDesactivar}
        pie={
          <BotonCargando type="submit" className="btn btn-danger" cargando={ocupadoDes} textoCargando="Desactivando...">
            Desactivar
          </BotonCargando>
        }
      >
        {modalDs.alerta && (
          <Alerta tipo={modalDs.alerta.tipo} mensaje={modalDs.alerta.mensaje} detalles={modalDs.alerta.detalles} onCerrar={modalDs.limpiar} />
        )}
        <p className="small">Se desactivará el vehículo con placa {placaDes}. No se borrarán sus datos.</p>
        <Campo label="Motivo" required htmlFor="motivoDesactivarVeh" error={errorMotivo}>
          <textarea id="motivoDesactivarVeh" name="motivo" className={`form-control${errorMotivo ? ' is-invalid' : ''}`} rows="3" required minLength="5" maxLength="250" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
        </Campo>
      </Modal>
      {zona}
    </Layout>
  );
}

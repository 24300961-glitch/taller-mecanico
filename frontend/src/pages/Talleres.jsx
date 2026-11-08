import { useEffect, useRef, useState } from 'react';
import { api, RX, texto } from '../api.js';
import Layout from '../components/Layout.jsx';
import { Alerta, BotonCargando, Campo, enfocarPrimerError, EstadoBadge, useAlerta, useToasts } from '../components/ui.jsx';

const VACIO_TALLER = {
  nombre: '', rfc: '', email: '', telefono: '', calle: '', numero: '',
  codigoPostal: '', estado: '', municipio: '', colonia: '', activo: 'true',
};

function direccion(t) {
  return [t.calle, t.numero, t.colonia, t.codigoPostal, t.municipio, t.estado].filter(Boolean).join(', ');
}

export default function Talleres() {
  const [lista, setLista] = useState([]);
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(VACIO_TALLER);
  const [errores, setErrores] = useState({});
  const [ocupadoGuardar, setOcupadoGuardar] = useState(false);
  const [fotoNombre, setFotoNombre] = useState('');
  const [preview, setPreview] = useState(null);
  const [fotoActual, setFotoActual] = useState(null);
  const [estados, setEstados] = useState([]);
  const [municipios, setMunicipios] = useState([]);
  const [colonias, setColonias] = useState([]);
  const [cpPorColonia, setCpPorColonia] = useState({});
  const [avisoCp, setAvisoCp] = useState('');
  const inputFoto = useRef(null);
  const general = useAlerta();
  const formAlerta = useAlerta();
  const { push, zona } = useToasts();

  const cargarTalleres = async () => {
    try {
      setLista((await api('/api/talleres?incluirInactivos=true')) || []);
    } catch (e) {
      general.mostrarError(e);
      setLista([]);
    }
  };

  const cargarEstados = async () => {
    try {
      setEstados((await api('/api/sepomex/estados')) || []);
    } catch (e) {
      setAvisoCp(e.message);
    }
  };

  const cargarMunicipios = async (estado, conservar) => {
    setMunicipios([]);
    setColonias([]);
    setCpPorColonia({});
    if (!conservar) setForm((f) => ({ ...f, municipio: '', colonia: '' }));
    if (!estado) return;
    try {
      setMunicipios((await api(`/api/sepomex/estados/${encodeURIComponent(estado)}/municipios`)) || []);
    } catch (e) {
      /* placeholder */
    }
  };

  const cargarColonias = async (estado, municipio, conservar) => {
    setColonias([]);
    setCpPorColonia({});
    if (!conservar) setForm((f) => ({ ...f, colonia: '' }));
    if (!estado || !municipio) return;
    try {
      const cols = await api(`/api/sepomex/estados/${encodeURIComponent(estado)}/municipios/${encodeURIComponent(municipio)}/colonias`);
      const mapa = {};
      (cols || []).forEach((c) => {
        if (!(c.colonia in mapa)) mapa[c.colonia] = c.codigoPostal;
      });
      setCpPorColonia(mapa);
      setColonias((cols || []).map((c) => c.colonia));
    } catch (e) {
      /* placeholder */
    }
  };

  const autocompletarCp = async (cp) => {
    if (!RX.CP.test(cp)) return;
    try {
      const opciones = await api(`/api/sepomex/cp/${encodeURIComponent(cp)}`);
      const p = opciones[0];
      let ests = estados;
      if (!ests.includes(p.estado)) {
        ests = (await api('/api/sepomex/estados')) || [];
        setEstados(ests);
      }
      setForm((f) => ({ ...f, estado: ests.includes(p.estado) ? p.estado : f.estado }));
      await cargarMunicipios(p.estado, true);
      const muns = (await api(`/api/sepomex/estados/${encodeURIComponent(p.estado)}/municipios`).catch(() => [])) || [];
      setMunicipios(muns);
      await cargarColonias(p.estado, p.municipio, true);
      const cols = (await api(`/api/sepomex/estados/${encodeURIComponent(p.estado)}/municipios/${encodeURIComponent(p.municipio)}/colonias`).catch(() => [])) || [];
      const mapa = {};
      cols.forEach((c) => {
        if (!(c.colonia in mapa)) mapa[c.colonia] = c.codigoPostal;
      });
      setCpPorColonia(mapa);
      setColonias(cols.map((c) => c.colonia));
      setForm((f) => ({
        ...f,
        municipio: muns.includes(p.municipio) ? p.municipio : '',
        colonia: cols.map((c) => c.colonia).includes(p.colonia) ? p.colonia : '',
      }));
      setAvisoCp(`${opciones.length} opción(es) SEPOMEX cargadas para el CP ${cp}; puedes corregir estos datos.`);
    } catch (e) {
      setAvisoCp(e.message);
    }
  };

  useEffect(() => {
    cargarEstados();
    cargarTalleres();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const modoAlta = () => {
    setEditandoId(null);
    setForm(VACIO_TALLER);
    setErrores({});
    formAlerta.limpiar();
    setFotoNombre('');
    setPreview(null);
    setFotoActual(null);
    if (inputFoto.current) inputFoto.current.value = '';
    setForm((f) => ({ ...f, estado: '' }));
    setMunicipios([]);
    setColonias([]);
    setCpPorColonia({});
    setAvisoCp('');
  };

  const modoEdicion = async (t) => {
    setEditandoId(t.id);
    setErrores({});
    formAlerta.limpiar();
    setForm({
      nombre: t.nombre || '', rfc: t.rfc || '', email: t.email || '', telefono: t.telefono || '',
      calle: t.calle || '', numero: t.numero || '', codigoPostal: t.codigoPostal || '',
      estado: '', municipio: '', colonia: '', activo: t.activo ? 'true' : 'false',
    });
    setFotoNombre('');
    setPreview(null);
    if (inputFoto.current) inputFoto.current.value = '';
    let ests = estados;
    if (!ests.includes(t.estado || '')) {
      ests = (await api('/api/sepomex/estados').catch(() => [])) || [];
      setEstados(ests);
    }
    const est = ests.includes(t.estado || '') ? t.estado : '';
    setForm((f) => ({ ...f, estado: est }));
    if (est) {
      const muns = (await api(`/api/sepomex/estados/${encodeURIComponent(est)}/municipios`).catch(() => [])) || [];
      setMunicipios(muns);
      const mun = muns.includes(t.municipio || '') ? t.municipio : '';
      setForm((f) => ({ ...f, municipio: mun }));
      if (mun) {
        const cols = (await api(`/api/sepomex/estados/${encodeURIComponent(est)}/municipios/${encodeURIComponent(mun)}/colonias`).catch(() => [])) || [];
        const mapa = {};
        cols.forEach((c) => {
          if (!(c.colonia in mapa)) mapa[c.colonia] = c.codigoPostal;
        });
        setCpPorColonia(mapa);
        setColonias(cols.map((c) => c.colonia));
        setForm((f) => ({ ...f, colonia: cols.map((c) => c.colonia).includes(t.colonia || '') ? t.colonia : '' }));
      }
    }
    setFotoActual(t.fotoPath || null);
    document.getElementById('formTaller')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const alElegirFoto = (archivo) => {
    setFotoNombre(archivo ? archivo.name : '');
    if (!archivo) {
      setPreview(null);
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => setPreview(e.target.result);
    reader.readAsDataURL(archivo);
  };

  const quitarFoto = () => {
    if (inputFoto.current) inputFoto.current.value = '';
    setFotoNombre('');
    setPreview(null);
  };

  const setF = (k) => (e) => {
    const v = e.target.value;
    if (k === 'estado') {
      setForm((f) => ({ ...f, estado: v, municipio: '', colonia: '' }));
      cargarMunicipios(v);
      return;
    }
    if (k === 'municipio') {
      setForm((f) => ({ ...f, municipio: v, colonia: '' }));
      cargarColonias(form.estado, v);
      return;
    }
    if (k === 'colonia') {
      const cp = cpPorColonia[v];
      setForm((f) => ({ ...f, colonia: v, codigoPostal: cp || f.codigoPostal }));
      return;
    }
    if (k === 'codigoPostal') {
      setForm((f) => ({ ...f, codigoPostal: v }));
      autocompletarCp(v.trim());
      return;
    }
    setForm((f) => ({ ...f, [k]: v }));
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    formAlerta.limpiar();
    const e = {};
    if (!form.nombre.trim()) e.nombre = 'El nombre es obligatorio';
    if (!form.rfc.trim()) e.rfc = 'El RFC es obligatorio';
    else if (!/^[A-ZÑ&]{3,4}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[A-Z0-9]{3}$/i.test(form.rfc.trim())) e.rfc = 'El RFC debe tener 12 o 13 caracteres válidos con fecha real';
    if (!form.email.trim()) e.email = 'El correo es obligatorio';
    if (!form.telefono.trim()) e.telefono = 'El teléfono es obligatorio';
    else if (!/\d/.test(form.telefono)) e.telefono = 'El teléfono debe incluir al menos un dígito.';
    if (!form.calle.trim()) e.calle = 'La calle es obligatoria';
    if (!form.numero.trim()) e.numero = 'El número es obligatorio';
    if (!form.codigoPostal.trim()) e.codigoPostal = 'El código postal es obligatorio';
    else if (!RX.CP.test(form.codigoPostal.trim())) e.codigoPostal = 'El código postal debe tener 5 dígitos';
    if (!form.estado) e.estado = 'El estado es obligatorio';
    if (!form.municipio) e.municipio = 'El municipio es obligatorio';
    if (!form.colonia) e.colonia = 'La colonia es obligatoria';
    const archivo = inputFoto.current && inputFoto.current.files[0];
    if (archivo && (archivo.size > 15 * 1024 * 1024 || !/\.(jpg|jpeg|png|webp)$/i.test(archivo.name))) {
      e.foto = archivo.size > 15 * 1024 * 1024 ? 'La foto no debe exceder 15 MB.' : 'Selecciona un archivo JPG, JPEG, PNG o WEBP.';
    }
    setErrores(e);
    if (Object.keys(e).length > 0) {
      push('Corrige los campos marcados antes de continuar.', false);
      enfocarPrimerError();
      return;
    }
    const cp = form.codigoPostal.trim();
    if (RX.CP.test(cp) && form.estado) {
      try {
        const ops = await api(`/api/sepomex/cp/${encodeURIComponent(cp)}`);
        const coincide = ops.some((o) => (o.estado || '').toLowerCase() === form.estado.toLowerCase());
        if (!coincide) {
          setErrores({
            estado: `El CP ${cp} no pertenece a este estado según SEPOMEX.`,
            codigoPostal: 'Revisa que el código postal corresponda al estado.',
          });
          return;
        }
      } catch (err) {
        if (err.status && err.status !== 404) {
          setErrores({ codigoPostal: 'No se pudo validar el código postal. Inténtalo de nuevo.' });
          return;
        }
      }
    }
    const fd = new FormData();
    fd.append('nombre', form.nombre.trim().toLowerCase());
    fd.append('rfc', form.rfc.trim().toUpperCase());
    fd.append('email', form.email.trim().toLowerCase());
    fd.append('telefono', form.telefono.trim().toLowerCase());
    fd.append('calle', form.calle.trim().toLowerCase());
    fd.append('numero', form.numero.trim().toLowerCase());
    fd.append('codigoPostal', cp);
    fd.append('estado', form.estado.trim().toLowerCase());
    fd.append('municipio', form.municipio.trim().toLowerCase());
    fd.append('colonia', form.colonia.trim().toLowerCase());
    fd.append('activo', form.activo);
    if (archivo) fd.append('foto', archivo);
    setOcupadoGuardar(true);
    try {
      const url = editandoId ? `/api/talleres/${editandoId}` : '/api/talleres';
      const r = await api(url, { method: editandoId ? 'PUT' : 'POST', body: fd });
      const resumen = [r.nombre, `RFC ${r.rfc}`, r.email, r.telefono, direccion(r), r.activo ? 'activo' : 'inactivo', r.fotoPath ? 'foto guardada' : 'sin foto'].join(' · ');
      general.mostrar('success', `${r.message} ${resumen}`);
      push(r.message, true);
      modoAlta();
      await cargarTalleres();
    } catch (err) {
      if (err.data && err.data.tallerExistente) {
        const ex = err.data.tallerExistente;
        formAlerta.mostrar('warning', `${err.message} Existente: ${ex.nombre} · RFC ${ex.rfc} · ${direccion(ex)}`);
      } else {
        const detalles = err.data && err.data.errores ? Object.values(err.data.errores) : [];
        formAlerta.mostrar(err.status === 409 ? 'warning' : 'danger', `No se guardó el taller: ${err.message}`, detalles);
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

  const cambiarEstatus = async (t) => {
    try {
      const r = await api(`/api/talleres/${t.id}${t.activo ? '/desactivar' : '/reactivar'}`, { method: 'PATCH' });
      general.mostrar('success', r.message);
      await cargarTalleres();
    } catch (e) {
      general.mostrarError(e);
    }
  };

  return (
    <Layout titulo="Talleres" subtitulo="Registro y estatus de talleres">
      {general.alerta && (
        <Alerta tipo={general.alerta.tipo} mensaje={general.alerta.mensaje} detalles={general.alerta.detalles} onCerrar={general.limpiar} />
      )}
      <div className="row g-4">
        <div className="col-xl-7">
          <div className="data-card shadow-sm">
            <div className="card-header d-flex justify-content-between align-items-center">
              <h2 className="h6 fw-bold mb-0">Talleres registrados</h2>
              <button className="btn btn-outline-primary btn-sm" type="button" onClick={cargarTalleres}>
                Actualizar
              </button>
            </div>
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>Taller</th>
                    <th>RFC</th>
                    <th>Dirección</th>
                    <th>Estado</th>
                    <th />
                  </tr>
                </thead>
                <tbody id="tablaTalleres">
                  {lista.length === 0 && (
                    <tr>
                      <td colSpan="5" className="empty-state">
                        Aún no hay talleres registrados.
                      </td>
                    </tr>
                  )}
                  {lista.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <div className="d-flex align-items-center">
                          {t.fotoPath && <img src={`/storage/${t.fotoPath}`} alt={`Foto de ${t.nombre || 'taller'}`} className="img-thumbnail me-2 align-middle img-thumb-tabla" />}
                          <span className="align-middle">
                            <div className="fw-semibold">{t.nombre}</div>
                            <div className="small text-muted">{t.email || ''}</div>
                          </span>
                        </div>
                      </td>
                      <td>{t.rfc || '—'}</td>
                      <td>{direccion(t) || 'Dirección pendiente'}</td>
                      <td>
                        <EstadoBadge activo={t.activo} textoActivo="Activo" textoInactivo="Inactivo" />
                      </td>
                      <td className="text-end">
                        <div className="d-flex gap-1 justify-content-end">
                          <button type="button" className="btn btn-sm btn-outline-primary me-1" onClick={() => modoEdicion(t)}>
                            Editar
                          </button>
                          <button type="button" className="btn btn-sm btn-outline-secondary me-1" onClick={() => cambiarEstatus(t)}>
                            {t.activo ? 'Desactivar' : 'Reactivar'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div className="col-xl-5">
          <div className="data-card shadow-sm">
            <div className="card-header d-flex justify-content-between align-items-center">
              <h2 className="h6 fw-bold mb-0">{editandoId ? 'Editar taller' : 'Registrar taller'}</h2>
              {editandoId && (
                <button className="btn btn-sm btn-outline-secondary" type="button" onClick={modoAlta}>
                  Cancelar edición
                </button>
              )}
            </div>
            <form id="formTaller" className="p-3 p-md-4" noValidate encType="multipart/form-data" onSubmit={guardar}>
              {formAlerta.alerta && (
                <Alerta tipo={formAlerta.alerta.tipo} mensaje={formAlerta.alerta.mensaje} detalles={formAlerta.alerta.detalles} onCerrar={formAlerta.limpiar} />
              )}
              <Campo label="Nombre" required htmlFor="tNombre" error={errores.nombre}>
                <input className={`form-control${errores.nombre ? ' is-invalid' : ''}`} id="tNombre" name="nombre" maxLength="120" required value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
              </Campo>
              <Campo label="RFC" required htmlFor="tRfc" error={errores.rfc}>
                <input className={`form-control text-uppercase${errores.rfc ? ' is-invalid' : ''}`} id="tRfc" name="rfc" maxLength="13" required value={form.rfc} onChange={(e) => setForm({ ...form, rfc: e.target.value })} />
              </Campo>
              <Campo label="Email" required htmlFor="tEmail" error={errores.email}>
                <input className={`form-control${errores.email ? ' is-invalid' : ''}`} id="tEmail" name="email" type="email" maxLength="190" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </Campo>
              <Campo label="Teléfono" required htmlFor="tTelefono" error={errores.telefono}>
                <input className={`form-control${errores.telefono ? ' is-invalid' : ''}`} id="tTelefono" name="telefono" maxLength="20" inputMode="tel" required value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} />
              </Campo>
              <div className="mb-3">
                <label className="form-label" htmlFor="tFoto">Foto (opcional, JPG/JPEG/PNG/WEBP, máximo 15 MB)</label>
                <input
                  className={`form-control${errores.foto ? ' is-invalid' : ''}`} id="tFoto" name="foto" type="file"
                  accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" ref={inputFoto}
                  onChange={(e) => alElegirFoto(e.target.files[0])}
                />
                {errores.foto && <div className="invalid-feedback d-block">{errores.foto}</div>}
                {preview && (
                  <div className="mt-2">
                    <img src={preview} alt="Vista previa de la foto del taller" className="img-thumbnail img-preview-taller" />
                    <button type="button" className="btn btn-sm btn-outline-danger ms-2" onClick={quitarFoto}>
                      Quitar
                    </button>
                  </div>
                )}
                {fotoActual && !preview && (
                  <div className="mt-2">
                    <p className="small text-muted mb-1">Foto actual:</p>
                    <img src={`/storage/${fotoActual}`} alt="Foto actual del taller" className="img-thumbnail img-actual-taller" />
                    <p className="small text-muted mt-1 mb-0">Selecciona un nuevo archivo para reemplazarla.</p>
                  </div>
                )}
                {fotoNombre && <div className="form-text">{fotoNombre}</div>}
              </div>
              <fieldset className="border rounded p-3 mb-3">
                <legend className="float-none w-auto px-2 fs-6 fw-semibold">Dirección *</legend>
                <Campo label="Calle" required htmlFor="tCalle" error={errores.calle}>
                  <input className={`form-control${errores.calle ? ' is-invalid' : ''}`} id="tCalle" name="calle" maxLength="160" required value={form.calle} onChange={(e) => setForm({ ...form, calle: e.target.value })} />
                </Campo>
                <Campo label="Número" required htmlFor="tNumero" error={errores.numero}>
                  <input className={`form-control${errores.numero ? ' is-invalid' : ''}`} id="tNumero" name="numero" maxLength="40" required value={form.numero} onChange={(e) => setForm({ ...form, numero: e.target.value })} />
                </Campo>
                <Campo label="Código postal" required htmlFor="tCp" error={errores.codigoPostal}>
                  <input className={`form-control${errores.codigoPostal ? ' is-invalid' : ''}`} id="tCp" name="codigoPostal" inputMode="numeric" maxLength="5" required value={form.codigoPostal} onChange={setF('codigoPostal')} />
                  <div className="form-text">Escribe 5 dígitos y el estado, municipio y colonia se cargan solos del catálogo SEPOMEX.</div>
                </Campo>
                <Campo label="Estado" required htmlFor="tEstado" error={errores.estado}>
                  <select id="tEstado" name="estado" className={`form-select${errores.estado ? ' is-invalid' : ''}`} required value={form.estado} onChange={setF('estado')}>
                    <option value="">Selecciona un estado</option>
                    {estados.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </Campo>
                <Campo label="Municipio" required htmlFor="tMunicipio" error={errores.municipio}>
                  <select id="tMunicipio" name="municipio" className={`form-select${errores.municipio ? ' is-invalid' : ''}`} required disabled={!municipios.length} value={form.municipio} onChange={setF('municipio')}>
                    <option value="">{municipios.length ? 'Selecciona un municipio' : 'Selecciona un estado primero'}</option>
                    {municipios.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </Campo>
                <Campo label="Colonia" required htmlFor="tColonia" error={errores.colonia}>
                  <select id="tColonia" name="colonia" className={`form-select${errores.colonia ? ' is-invalid' : ''}`} required disabled={!colonias.length} value={form.colonia} onChange={setF('colonia')}>
                    <option value="">{colonias.length ? 'Selecciona una colonia' : 'Selecciona un municipio primero'}</option>
                    {colonias.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </Campo>
                <div className="form-text mt-2" aria-live="polite">
                  {avisoCp || 'El CP se llena solo al elegir colonia; también puedes escribir 5 dígitos para autocompletar todo.'}
                </div>
              </fieldset>
              <Campo label="Estatus" required htmlFor="tActivo">
                <select id="tActivo" name="activo" className="form-select" required value={form.activo} onChange={(e) => setForm({ ...form, activo: e.target.value })}>
                  <option value="true">Activo</option>
                  <option value="false">Inactivo</option>
                </select>
              </Campo>
              <BotonCargando type="submit" className="btn btn-accent w-100" cargando={ocupadoGuardar} textoCargando={editandoId ? 'Guardando cambios...' : 'Guardando...'}>
                {editandoId ? 'Guardar cambios' : 'Registrar taller'}
              </BotonCargando>
            </form>
          </div>
        </div>
      </div>
      {zona}
    </Layout>
  );
}

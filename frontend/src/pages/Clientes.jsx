import { useEffect, useRef, useState } from 'react';
import { api, digitos, fechaCorta, RX, telefonoLegible, texto } from '../api.js';
import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';
import ComboTaller from '../components/ComboTaller.jsx';
import { Alerta, BotonCargando, Campo, enfocarPrimerError, EstadoBadge, Modal, useAlerta, useToasts } from '../components/ui.jsx';

const TAMANO_PAGINA = 10;
const MAX_FOTOS_VEH = 5;
const MAX_BYTES_FOTO = 15 * 1024 * 1024;
const EXTENSIONES_FOTO = ['jpg', 'jpeg', 'png', 'webp'];

function primeraFotoVeh(fotos) {
  return (fotos || '').split(',').map((s) => s.trim()).filter(Boolean)[0] || '';
}

const VACIO_CLIENTE = {
  nombre: '', apellidoPaterno: '', apellidoMaterno: '', telefono: '', telefonoAlterno: '',
  telefonoPersonal: '', telefonoTrabajo: '', nombreContacto: '', telefonoContacto: '',
  email: '', rfc: '', direccion: '', calle: '', numero: '', codigoPostal: '', estado: '',
  municipio: '', colonia: '',
};

function validarCliente(d) {
  const e = {};
  if (d.nombre && !RX.NOMBRE_80.test(d.nombre)) e.nombre = 'El nombre debe tener de 2 a 80 letras';
  if (d.apellidoPaterno && !RX.NOMBRE_80.test(d.apellidoPaterno)) e.apellidoPaterno = 'El apellido paterno debe tener de 2 a 80 letras';
  if (d.apellidoMaterno && !RX.NOMBRE_80.test(d.apellidoMaterno)) e.apellidoMaterno = 'El apellido materno debe tener de 2 a 80 letras';
  const tels = { telefono: d.telefono, telefonoAlterno: d.telefonoAlterno, telefonoPersonal: d.telefonoPersonal, telefonoTrabajo: d.telefonoTrabajo, telefonoContacto: d.telefonoContacto };
  const vistos = {};
  Object.keys(tels).forEach((k) => {
    const v = tels[k];
    if (!v) return;
    if (!RX.TEL.test(v) || digitos(v).length !== 10) e[k] = 'Debe tener exactamente 10 dígitos';
    else if (vistos[digitos(v)]) e[k] = 'Los teléfonos deben ser diferentes entre sí';
    else vistos[digitos(v)] = true;
  });
  if (!d.telefono) e.telefono = 'El teléfono es obligatorio';
  if (!d.email) e.email = 'El correo es obligatorio para identificar al cliente';
  if ((d.telefonoContacto ? 1 : 0) !== (d.nombreContacto ? 1 : 0)) {
    e.nombreContacto = 'El contacto alternativo requiere nombre y teléfono juntos';
    e.telefonoContacto = 'El contacto alternativo requiere nombre y teléfono juntos';
  }
  if (d.nombreContacto && !RX.NOMBRE_80.test(d.nombreContacto)) e.nombreContacto = 'El nombre del contacto solo admite letras';
  if (d.rfc && !RX.RFC.test(d.rfc)) e.rfc = 'El RFC no tiene un formato válido (12 o 13 caracteres)';
  if (d.codigoPostal && !RX.CP.test(d.codigoPostal)) e.codigoPostal = 'El código postal debe tener 5 dígitos';
  return e;
}

function Dato({ etiqueta, valor }) {
  return (
    <>
      <dt className="col-sm-4 text-muted small fw-normal">{etiqueta}</dt>
      <dd className="col-sm-8 small">{texto(valor)}</dd>
    </>
  );
}

export default function Clientes() {
  const { tienePermiso } = useAuth();
  const P = {
    registrar: tienePermiso('CLIENTES_REGISTRAR'),
    editar: tienePermiso('CLIENTES_EDITAR'),
    desactivar: tienePermiso('CLIENTES_DESACTIVAR'),
    vConsultar: tienePermiso('VEHICULOS_CONSULTAR'),
    vRegistrar: tienePermiso('VEHICULOS_REGISTRAR'),
    vEditar: tienePermiso('VEHICULOS_EDITAR'),
    vDesactivar: tienePermiso('VEHICULOS_DESACTIVAR'),
  };
  const [q, setQ] = useState('');
  const [inactivos, setInactivos] = useState(false);
  const [lista, setLista] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [total, setTotal] = useState(0);
  const [ocupadoBuscar, setOcupadoBuscar] = useState(false);
  const [cliente, setCliente] = useState(null);
  const general = useAlerta();
  const { push, zona } = useToasts();

  // Modal cliente
  const [modalCliente, setModalCliente] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [form, setForm] = useState(VACIO_CLIENTE);
  const [errores, setErrores] = useState({});
  const [talleresAlta, setTalleresAlta] = useState([]);
  const [tallerIds, setTallerIds] = useState([]);
  const [comboTalleres, setComboTalleres] = useState(false);
  const [buscaTaller, setBuscaTaller] = useState('');
  const comboRef = useRef(null);

  useEffect(() => {
    if (!comboTalleres) return;
    const cerrar = (e) => {
      if (comboRef.current && !comboRef.current.contains(e.target)) setComboTalleres(false);
    };
    const tecla = (e) => {
      if (e.key === 'Escape') setComboTalleres(false);
    };
    document.addEventListener('mousedown', cerrar);
    document.addEventListener('keydown', tecla);
    return () => {
      document.removeEventListener('mousedown', cerrar);
      document.removeEventListener('keydown', tecla);
    };
  }, [comboTalleres]);

  const alternarTaller = (idStr) => {
    setTallerIds((prev) => (prev.includes(idStr) ? prev.filter((x) => x !== idStr) : [...prev, idStr]));
  };

  const talleresFiltrados = talleresAlta.filter((t) =>
    (t.nombre || '').toLowerCase().includes(buscaTaller.trim().toLowerCase())
  );
  const [ocupadoGuardar, setOcupadoGuardar] = useState(false);
  const [estados, setEstados] = useState([]);
  const [municipios, setMunicipios] = useState([]);
  const [colonias, setColonias] = useState([]);
  const [cpPorColonia, setCpPorColonia] = useState({});
  const [avisoCp, setAvisoCp] = useState('');
  const modalCl = useAlerta();

  // Modal vehículo
  const [modalVeh, setModalVeh] = useState(false);
  const [vehEditId, setVehEditId] = useState(null);
  const [formV, setFormV] = useState({ tallerId: '', placa: '', marca: '', modelo: '', anio: '', vin: '', color: '', version: '', kilometraje: '', fotos: '' });
  const [erroresV, setErroresV] = useState({});
  const [talleresVeh, setTalleresVeh] = useState([]);
  const [ocupadoV, setOcupadoV] = useState(false);
  const [subiendoFotosV, setSubiendoFotosV] = useState(false);
  const [errorFotosV, setErrorFotosV] = useState('');
  const modalVh = useAlerta();

  // Modal desactivar
  const [modalDes, setModalDes] = useState(false);
  const [objetivo, setObjetivo] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [errorMotivo, setErrorMotivo] = useState('');
  const [ocupadoDes, setOcupadoDes] = useState(false);
  const modalDs = useAlerta();

  const buscar = async (pag) => {
    const p = typeof pag === 'number' ? Math.max(0, pag) : pagina;
    setPagina(p);
    setOcupadoBuscar(true);
    try {
      const r = await api(`/api/clientes/pagina?q=${encodeURIComponent(q.trim())}&incluirInactivos=${inactivos}&page=${p}&size=${TAMANO_PAGINA}`);
      setLista(r.contenido || []);
      setTotalPaginas(r.totalPaginas || 0);
      setTotal(r.totalElementos || 0);
    } catch (e) {
      general.mostrarError(e);
      setLista([]);
    } finally {
      setOcupadoBuscar(false);
    }
  };

  useEffect(() => {
    buscar(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const verCliente = async (id) => {
    try {
      const c = await api(`/api/clientes/${id}`);
      setCliente(c);
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const cargarEstados = async () => {
    try {
      setEstados((await api('/api/sepomex/estados')) || []);
    } catch (e) {
      /* se reintenta al abrir */
    }
  };

  const cargarMunicipios = async (estado) => {
    setMunicipios([]);
    setColonias([]);
    setCpPorColonia({});
    setForm((f) => ({ ...f, municipio: '', colonia: '' }));
    if (!estado) return;
    try {
      setMunicipios((await api(`/api/sepomex/estados/${encodeURIComponent(estado)}/municipios`)) || []);
    } catch (e) {
      /* vacío */
    }
  };

  const cargarColonias = async (estado, municipio) => {
    setColonias([]);
    setCpPorColonia({});
    setForm((f) => ({ ...f, colonia: '' }));
    if (!estado || !municipio) return;
    try {
      const cols = await api(`/api/sepomex/estados/${encodeURIComponent(estado)}/municipios/${encodeURIComponent(municipio)}/colonias`);
      const mapa = {};
      (cols || []).forEach((x) => {
        if (!(x.colonia in mapa)) mapa[x.colonia] = x.codigoPostal;
      });
      setCpPorColonia(mapa);
      setColonias((cols || []).map((x) => x.colonia));
    } catch (e) {
      /* vacío */
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
      const muns = (await api(`/api/sepomex/estados/${encodeURIComponent(p.estado)}/municipios`)) || [];
      setMunicipios(muns);
      const cols = (await api(`/api/sepomex/estados/${encodeURIComponent(p.estado)}/municipios/${encodeURIComponent(p.municipio)}/colonias`)) || [];
      const mapa = {};
      cols.forEach((x) => {
        if (!(x.colonia in mapa)) mapa[x.colonia] = x.codigoPostal;
      });
      setCpPorColonia(mapa);
      setColonias(cols.map((x) => x.colonia));
      setForm((f) => ({
        ...f,
        municipio: muns.includes(p.municipio) ? p.municipio : '',
        colonia: cols.map((x) => x.colonia).includes(p.colonia) ? p.colonia : '',
      }));
      setAvisoCp(`${opciones.length} opción(es) SEPOMEX cargadas para el CP ${cp}; puedes corregir estos datos.`);
    } catch (e) {
      setAvisoCp(e.message);
    }
  };

  const abrirNuevo = async () => {
    general.limpiar();
    try {
      const alta = await api('/api/clientes/alta');
      setTalleresAlta(alta.talleres || []);
      setEditandoId(null);
      setForm(VACIO_CLIENTE);
      setErrores({});
      setTallerIds([]);
      setBuscaTaller('');
      setComboTalleres(false);
      setAvisoCp('');
      modalCl.limpiar();
      await cargarEstados();
      setModalCliente(true);
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const abrirEdicion = async (c) => {
    setEditandoId(c.id);
    setForm({
      nombre: c.nombre || '', apellidoPaterno: c.apellidoPaterno || '', apellidoMaterno: c.apellidoMaterno || '',
      telefono: c.telefono || '', telefonoAlterno: c.telefonoAlterno || '', telefonoPersonal: c.telefonoPersonal || '',
      telefonoTrabajo: c.telefonoTrabajo || '', nombreContacto: c.nombreContacto || '', telefonoContacto: c.telefonoContacto || '',
      email: c.email || '', rfc: c.rfc || '', direccion: c.direccion || '', calle: c.calle || '', numero: c.numero || '',
      codigoPostal: c.codigoPostal || '', estado: '', municipio: '', colonia: '',
    });
    setErrores({});
    setAvisoCp('');
    modalCl.limpiar();
    setModalCliente(true);
    let ests = estados;
    if (!ests.includes(c.estado || '')) {
      ests = (await api('/api/sepomex/estados').catch(() => [])) || [];
      setEstados(ests);
    }
    setForm((f) => ({ ...f, estado: ests.includes(c.estado || '') ? c.estado : '' }));
    if (c.estado) {
      const muns = (await api(`/api/sepomex/estados/${encodeURIComponent(c.estado)}/municipios`).catch(() => [])) || [];
      setMunicipios(muns);
      setForm((f) => ({ ...f, municipio: muns.includes(c.municipio || '') ? c.municipio : '' }));
      if (c.municipio) {
        const cols = (await api(`/api/sepomex/estados/${encodeURIComponent(c.estado)}/municipios/${encodeURIComponent(c.municipio)}/colonias`).catch(() => [])) || [];
        const mapa = {};
        cols.forEach((x) => {
          if (!(x.colonia in mapa)) mapa[x.colonia] = x.codigoPostal;
        });
        setCpPorColonia(mapa);
        setColonias(cols.map((x) => x.colonia));
        setForm((f) => ({ ...f, colonia: cols.map((x) => x.colonia).includes(c.colonia || '') ? c.colonia : '' }));
      }
    }
  };

  const guardarCliente = async () => {
    modalCl.limpiar();
    const d = { ...form };
    Object.keys(d).forEach((k) => {
      if (typeof d[k] === 'string') d[k] = d[k].trim();
    });
    const e = validarCliente(d);
    if (!d.nombre) e.nombre = 'El nombre es obligatorio';
    if (!d.apellidoPaterno) e.apellidoPaterno = 'El apellido paterno es obligatorio';
    if (!d.email) e.email = e.email || 'El correo es obligatorio para identificar al cliente';
    if (!d.calle) e.calle = 'La calle es obligatoria';
    if (!d.numero) e.numero = 'El número es obligatorio';
    if (!d.colonia) e.colonia = 'La colonia es obligatoria';
    if (!d.municipio) e.municipio = 'El municipio es obligatorio';
    if (!d.estado) e.estado = 'El estado es obligatorio';
    setErrores(e);
    if (Object.keys(e).length > 0) {
      modalCl.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      push('Corrige los campos marcados antes de continuar.', false);
      enfocarPrimerError();
      return;
    }
    if (RX.CP.test(d.codigoPostal) && d.estado) {
      try {
        const ops = await api(`/api/sepomex/cp/${encodeURIComponent(d.codigoPostal)}`);
        const coincide = ops.some((o) => (o.estado || '').toLowerCase() === d.estado.toLowerCase());
        if (!coincide) {
          setErrores({ estado: `El CP ${d.codigoPostal} no pertenece a este estado según SEPOMEX.`, codigoPostal: 'Revisa que el código postal corresponda al estado.' });
          return;
        }
      } catch (err) {
        if (err.status && err.status !== 404) {
          setErrores({ codigoPostal: 'No se pudo validar el código postal. Inténtalo de nuevo.' });
          return;
        }
      }
    }
    const datos = { ...d, confirmarCoincidencias: false };
    if (!editandoId) {
      const ids = tallerIds.map(Number).filter((n) => !Number.isNaN(n));
      if (!ids.length) {
        setErrores({ tallerIds: 'Selecciona al menos un taller activo.' });
        modalCl.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
        push('Selecciona al menos un taller activo.', false);
        enfocarPrimerError();
        return;
      }
      datos.tallerIds = ids;
    }
    setOcupadoGuardar(true);    try {
      const url = editandoId ? `/api/clientes/${editandoId}` : '/api/clientes';
      const r = await api(url, { method: editandoId ? 'PUT' : 'POST', body: datos });
      setModalCliente(false);
      push(r.message, true);
      general.mostrar('success', r.message);
      await buscar(0);
      await verCliente(r.id);
    } catch (err) {
      modalCl.mostrarError(err);
      if (err.data && err.data.errores) {
        const porCampo = {};
        Object.keys(err.data.errores).forEach((k) => {
          porCampo[k] = err.data.errores[k];
        });
        setErrores(porCampo);
      }
    } finally {
      setOcupadoGuardar(false);
    }
  };

  const abrirVehiculo = async (v) => {
    setFormV({ tallerId: '', placa: '', marca: '', modelo: '', anio: '', vin: '', color: '', version: '', kilometraje: '', fotos: '' });
    setErroresV({});
    setErrorFotosV('');
    setSubiendoFotosV(false);
    modalVh.limpiar();
    setVehEditId(v ? v.id : null);
    try {
      const lista = await api('/api/talleres/activos');
      setTalleresVeh(lista || []);
    } catch (e) {
      modalVh.mostrarError(e);
      return;
    }
    if (v) {
      let lista = [];
      try {
        lista = await api('/api/talleres/activos');
      } catch (e) {
        /* ya cargada */
      }
      const todas = lista.length ? lista : talleresVeh;
      let opciones = [...todas];
      if (v.tallerId && !opciones.some((o) => String(o.id) === String(v.tallerId))) {
        opciones = [...opciones, { id: v.tallerId, nombre: `${v.taller || 'Taller'} (asignación existente)` }];
      }
      setTalleresVeh(opciones);
      setFormV({
        tallerId: v.tallerId || '', placa: v.placa || '', marca: v.marca || '', modelo: v.modelo || '',
        anio: v.anio === null || v.anio === undefined ? '' : String(v.anio), vin: v.vin || '', color: v.color || '',
        version: v.version || '', kilometraje: v.kilometraje === null || v.kilometraje === undefined ? '' : String(v.kilometraje), fotos: v.fotos || '',
      });
    }
    setModalVeh(true);
  };

  const guardarVehiculo = async () => {
    modalVh.limpiar();
    const d = { ...formV, placa: (formV.placa || '').toUpperCase(), vin: (formV.vin || '').toUpperCase() };
    const e = {};
    if (!d.placa || d.placa.length < 4 || d.placa.length > 10 || !/^[A-Z0-9]+-[A-Z0-9]+(-[A-Z0-9]+)?$/.test(d.placa))
      e.placa = 'La placa debe ir en mayúsculas y separada por un guion (ej. ABC-123)';
    if (!d.marca) e.marca = 'La marca es obligatoria';
    else if (d.marca.length < 2 || d.marca.length > 40) e.marca = 'La marca debe tener entre 2 y 40 caracteres';
    if (!d.modelo) e.modelo = 'El modelo es obligatorio';
    if (!d.anio || !/^\d{4}$/.test(d.anio) || Number(d.anio) < 1900 || Number(d.anio) > new Date().getFullYear() + 1)
      e.anio = 'El año debe ser un número de 4 dígitos válido';
    if (!d.kilometraje) e.kilometraje = 'El kilometraje es obligatorio';
    if (d.vin && !/^[A-HJ-NPR-Z0-9]{17}$/i.test(d.vin)) e.vin = 'El VIN debe tener 17 caracteres alfanuméricos (sin I, O ni Q)';
    if (d.version && d.version.length > 30) e.version = 'La versión no debe exceder 30 caracteres';
    if (d.kilometraje && !/^\d+$/.test(d.kilometraje)) e.kilometraje = 'El kilometraje debe ser un entero mayor o igual a cero';
    if (d.fotos && d.fotos.length > 500) e.fotos = 'Las rutas de fotos no deben exceder 500 caracteres';
    if (!d.tallerId) e.tallerId = 'Selecciona un taller activo';
    setErroresV(e);
    if (Object.keys(e).length > 0) {
      modalVh.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      push('Corrige los campos marcados antes de continuar.', false);
      enfocarPrimerError();
      return;
    }
    setOcupadoV(true);
    try {
      const datos = {
        clienteId: Number(cliente.id), tallerId: Number(d.tallerId), placa: d.placa, marca: d.marca.trim(),
        modelo: d.modelo.trim(), anio: Number(d.anio), vin: d.vin.trim(), color: d.color.trim(),
        version: d.version.trim(), kilometraje: d.kilometraje === '' ? null : Number(d.kilometraje), fotos: d.fotos.trim(),
      };
      const url = vehEditId ? `/api/vehiculos/${vehEditId}` : '/api/vehiculos';
      const r = await api(url, { method: vehEditId ? 'PUT' : 'POST', body: datos });
      setModalVeh(false);
      push(r.message, true);
      if (cliente) await verCliente(cliente.id);
    } catch (err) {
      if (err.data && err.data.coincidencias) {
        const detalles = err.data.coincidencias.map((c) => `${c.placa} · propietario: ${c.cliente} (#${c.clienteId}) — ${c.motivos.join(', ')}`);
        modalVh.mostrar('warning', err.message, detalles);
        const ev = {};
        if (err.data.coincidencias.some((c) => c.motivos.indexOf('Misma placa') !== -1)) ev.placa = 'Placa ya registrada';
        if (err.data.coincidencias.some((c) => c.motivos.indexOf('Mismo VIN') !== -1)) ev.vin = 'VIN ya registrado';
        setErroresV(ev);
      } else {
        modalVh.mostrarError(err);
        if (err.data && err.data.errores) {
          const porCampo = {};
          Object.keys(err.data.errores).forEach((k) => {
            porCampo[k] = err.data.errores[k];
          });
          setErroresV(porCampo);
        }
      }
    } finally {
      setOcupadoV(false);
    }
  };

  const fotosActualesV = (formV.fotos || '').split(',').map((s) => s.trim()).filter(Boolean);

  const subirFotosV = async (ev) => {
    const archivos = Array.from(ev.target.files || []);
    ev.target.value = '';
    if (archivos.length === 0) return;
    if (fotosActualesV.length + archivos.length > MAX_FOTOS_VEH) {
      setErrorFotosV(`Puedes agregar hasta ${MAX_FOTOS_VEH} fotos por vehículo.`);
      return;
    }
    for (const f of archivos) {
      const ext = f.name.split('.').pop().toLowerCase();
      if (!EXTENSIONES_FOTO.includes(ext) || (f.type && !f.type.startsWith('image/'))) {
        setErrorFotosV('Solo se admiten fotos (JPG, PNG o WebP). No se permiten documentos.');
        return;
      }
      if (f.size > MAX_BYTES_FOTO) {
        setErrorFotosV(`La foto ${f.name} excede el tamaño máximo permitido de 15 MB.`);
        return;
      }
    }
    setErrorFotosV('');
    setSubiendoFotosV(true);
    try {
      const nuevas = [];
      for (const archivo of archivos) {
        const datos = new FormData();
        datos.append('foto', archivo);
        const r = await api('/api/vehiculos/fotos', { method: 'POST', body: datos });
        nuevas.push(r.ruta);
      }
      const total = [...fotosActualesV, ...nuevas].join(',');
      if (total.length > 500) {
        setErrorFotosV('Las fotos no deben exceder 500 caracteres en total.');
        return;
      }
      setFormV((actual) => ({ ...actual, fotos: total }));
    } catch (e) {
      setErrorFotosV(e.message || 'No se pudo subir la foto.');
    } finally {
      setSubiendoFotosV(false);
    }
  };

  const quitarFotoV = (ruta) => {
    setFormV((actual) => ({
      ...actual,
      fotos: (actual.fotos || '').split(',').map((s) => s.trim()).filter((x) => x && x !== ruta).join(','),
    }));
    setErrorFotosV('');
  };

  const abrirDesactivar = (tipo, id, descripcion) => {
    setObjetivo({ tipo, id, descripcion });
    setMotivo('');
    setErrorMotivo('');
    modalDs.limpiar();
    setModalDes(true);
    setTimeout(() => document.getElementById('dMotivo')?.focus(), 100);
  };

  const enviarDesactivar = async () => {
    modalDs.limpiar();
    const m = motivo.trim();
    if (!m || m.length < 5) {
      setErrorMotivo('El motivo debe tener al menos 5 caracteres');
      return;
    }
    setOcupadoDes(true);
    try {
      const base = objetivo.tipo === 'cliente' ? '/api/clientes/' : '/api/vehiculos/';
      const r = await api(`${base}${objetivo.id}/desactivar`, { method: 'PATCH', body: { motivo: m } });
      setModalDes(false);
      push(r.message, true);
      if (cliente) await verCliente(cliente.id);
      if (objetivo.tipo === 'cliente') await buscar();
    } catch (e) {
      modalDs.mostrarError(e);
      if (e.data && e.data.errores && e.data.errores.motivo) setErrorMotivo(e.data.errores.motivo);
    } finally {
      setOcupadoDes(false);
    }
  };

  const reactivar = async (tipo, id) => {
    const base = tipo === 'cliente' ? '/api/clientes/' : '/api/vehiculos/';
    try {
      const r = await api(`${base}${id}/reactivar`, { method: 'PATCH' });
      push(r.message, true);
      general.limpiar();
      if (cliente) await verCliente(cliente.id);
      if (tipo === 'cliente') await buscar();
    } catch (e) {
      general.mostrarError(e);
      push(e.message, false);
    }
  };

  const setF = (k) => (e) => {
    const v = e.target.value;
    setForm((f) => ({ ...f, [k]: v }));
    if (k === 'estado') cargarMunicipios(v);
    if (k === 'municipio') cargarColonias(form.estado, v);
    if (k === 'colonia') {
      const cp = cpPorColonia[v];
      if (cp) setForm((f) => ({ ...f, colonia: v, codigoPostal: cp }));
      return;
    }
    if (k === 'codigoPostal') autocompletarCp(v.trim());
  };

  const descripcionDesactivar =
    objetivo && objetivo.descripcion
      ? objetivo.descripcion
      : 'Se desactivará el vehículo.';

  return (
    <Layout titulo="Clientes" subtitulo="Alta, consulta, corrección y desactivación">
      {general.alerta && (
        <Alerta tipo={general.alerta.tipo} mensaje={general.alerta.mensaje} detalles={general.alerta.detalles} onCerrar={general.limpiar} />
      )}
      <div className="row g-4">
        <div className="col-xxl-7">
          <div className="data-card shadow-sm">
            <div className="card-header">
              <form className="row g-2 align-items-center" noValidate onSubmit={(ev) => { ev.preventDefault(); general.limpiar(); buscar(0); }}>
                <div className="col-md">
                  <label htmlFor="qCliente" className="visually-hidden">Buscar cliente</label>
                  <input
                    type="search" id="qCliente" name="q" className="form-control" maxLength="100"
                    placeholder="Buscar por nombre, teléfono, correo o ID" value={q} onChange={(e) => setQ(e.target.value)}
                  />
                </div>
                <div className="col-auto form-check ms-2">
                  <input
                    className="form-check-input" type="checkbox" id="inactivosCliente" checked={inactivos}
                    onChange={(e) => { setInactivos(e.target.checked); }}
                  />
                  <label className="form-check-label small" htmlFor="inactivosCliente">Incluir desactivados</label>
                </div>
                <div className="col-auto">
                  <BotonCargando type="submit" className="btn btn-outline-primary" cargando={ocupadoBuscar} textoCargando="Buscando...">
                    Buscar
                  </BotonCargando>
                </div>
                {P.registrar && (
                  <div className="col-auto">
                    <button type="button" id="btnNuevoCliente" className="btn btn-accent" onClick={abrirNuevo}>+ Nuevo cliente</button>
                  </div>
                )}
              </form>
            </div>
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Teléfono</th>
                    <th>Correo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody id="tablaClientes">
                  {lista.length === 0 && (
                    <tr>
                      <td colSpan="5" className="empty-state">
                        {q ? `No se encontraron clientes con "${q}".` : 'Aún no hay clientes registrados.'}
                      </td>
                    </tr>
                  )}
                  {lista.map((c) => (
                    <tr
                      key={c.id}
                      className={`fila-seleccionable${cliente && cliente.id === c.id ? ' table-active' : ''}`}
                      tabIndex="0"
                      onClick={() => verCliente(c.id)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') verCliente(c.id);
                      }}
                    >
                      <td className="small text-muted">#{c.id}</td>
                      <td className="fw-semibold">{c.nombreCompleto}</td>
                      <td className="small">{telefonoLegible(c.telefono)}</td>
                      <td className="small">{c.email}</td>
                      <td>
                        <EstadoBadge activo={c.activo} textoActivo="Activo" textoInactivo="Desactivado" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-3 py-2 small text-muted border-top" id="resumenClientes">{total} cliente(s) en total</div>
            <div className="px-3 py-2 border-top d-flex justify-content-between align-items-center gap-2">
              <button type="button" id="btnPaginaAnterior" className="btn btn-sm btn-outline-secondary" disabled={pagina <= 0} onClick={() => buscar(pagina - 1)}>
                ← Anterior
              </button>
              <span className="small text-muted" id="infoPagina">{totalPaginas ? `Página ${pagina + 1} de ${totalPaginas}` : 'Sin páginas'}</span>
              <button type="button" id="btnPaginaSiguiente" className="btn btn-sm btn-outline-secondary" disabled={pagina >= totalPaginas - 1} onClick={() => buscar(pagina + 1)}>
                Siguiente →
              </button>
            </div>
          </div>
        </div>
        <div className="col-xxl-5">
          <div className="data-card shadow-sm">
            <div className="card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
              <h2 className="fw-semibold mb-0 section-h5">
                {cliente ? `${cliente.nombreCompleto} (#${cliente.id})` : 'Detalle del cliente'}
              </h2>
              <div className="d-flex gap-2">
                {cliente && cliente.activo && P.editar && (
                  <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => abrirEdicion(cliente)}>
                    Editar
                  </button>
                )}
                {cliente && P.desactivar && cliente.activo && (
                  <button
                    type="button" className="btn btn-sm btn-outline-danger"
                    onClick={() => abrirDesactivar('cliente', cliente.id, `Se desactivará al cliente ${cliente.nombreCompleto}. No se borrarán sus datos y podrás reactivarlo después.`)}
                  >
                    Desactivar
                  </button>
                )}
                {cliente && P.desactivar && !cliente.activo && (
                  <button type="button" className="btn btn-sm btn-outline-success" onClick={() => reactivar('cliente', cliente.id)}>
                    Reactivar
                  </button>
                )}
              </div>
            </div>
            <div className="p-3 p-md-4">
              {!cliente && <div className="empty-state p-0">Selecciona un cliente de la lista para ver su información y vehículos.</div>}
              {cliente && (
                <>
                  {!cliente.activo && (
                    <div className="alert alert-secondary small">
                      Cliente desactivado el {fechaCorta(cliente.desactivadoEn)} por {texto(cliente.desactivadoPor)}. Motivo:{' '}
                      {texto(cliente.motivoDesactivacion)}
                    </div>
                  )}
                  <dl className="row mb-3">
                    <Dato etiqueta="Apellido paterno" valor={cliente.apellidoPaterno} />
                    <Dato etiqueta="Apellido materno" valor={cliente.apellidoMaterno} />
                    <Dato etiqueta="Teléfono" valor={telefonoLegible(cliente.telefono)} />
                    <Dato etiqueta="Teléfono alterno" valor={cliente.telefonoAlterno ? telefonoLegible(cliente.telefonoAlterno) : null} />
                    <Dato etiqueta="Teléfono personal" valor={cliente.telefonoPersonal ? telefonoLegible(cliente.telefonoPersonal) : null} />
                    <Dato etiqueta="Teléfono de trabajo" valor={cliente.telefonoTrabajo ? telefonoLegible(cliente.telefonoTrabajo) : null} />
                    <Dato etiqueta="Contacto alternativo" valor={cliente.nombreContacto} />
                    <Dato etiqueta="Teléfono del contacto" valor={cliente.telefonoContacto ? telefonoLegible(cliente.telefonoContacto) : null} />
                    <Dato etiqueta="Correo" valor={cliente.email} />
                    <Dato etiqueta="RFC" valor={cliente.rfc} />
                    <Dato etiqueta="Calle" valor={cliente.calle} />
                    <Dato etiqueta="Número" valor={cliente.numero} />
                    <Dato etiqueta="Colonia" valor={cliente.colonia} />
                    <Dato etiqueta="Código postal" valor={cliente.codigoPostal} />
                    <Dato etiqueta="Municipio" valor={cliente.municipio} />
                    <Dato etiqueta="Estado" valor={cliente.estado} />
                    <Dato etiqueta="Referencia" valor={cliente.direccion} />
                    <Dato
                      etiqueta="Talleres asociados"
                      valor={(cliente.talleres || []).map((t) => `${t.nombre}${t.activo ? '' : ' (inactivo)'}`).join(', ') || null}
                    />
                    <Dato etiqueta="Registrado" valor={`${fechaCorta(cliente.creadoEn)} por ${texto(cliente.creadoPor)}`} />
                    <Dato
                      etiqueta="Última modificación"
                      valor={cliente.actualizadoEn ? `${fechaCorta(cliente.actualizadoEn)} por ${texto(cliente.actualizadoPor)}` : null}
                    />
                  </dl>
                  {P.vConsultar && (
                    <>
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <h3 className="h6 fw-semibold mb-0">Vehículos ({(cliente.vehiculos || []).length})</h3>
                        {P.vRegistrar && cliente.activo && (
                          <button type="button" className="btn btn-sm btn-accent" onClick={() => abrirVehiculo(null)}>
                            + Agregar vehículo
                          </button>
                        )}
                      </div>
                      {(!cliente.vehiculos || cliente.vehiculos.length === 0) && (
                        <p className="small text-muted mb-0">Este cliente no tiene vehículos registrados.</p>
                      )}
                      {cliente.vehiculos && cliente.vehiculos.length > 0 && (
                        <div className="table-responsive">
                          <table className="table table-sm align-middle mb-0">
                            <tbody>
                              {cliente.vehiculos.map((v) => (
                                <tr key={v.id}>
                                  <td className="fw-semibold">{v.placa}</td>
                                  <td className="small">
                                    <div className="d-flex align-items-center gap-2">
                                      {primeraFotoVeh(v.fotos) ? (
                                        <img
                                          src={primeraFotoVeh(v.fotos)} alt="Foto del vehículo"
                                          style={{ width: 46, height: 36, objectFit: 'cover', borderRadius: 4, border: '1px solid #dee2e6' }}
                                        />
                                      ) : (
                                        <span className="text-muted text-center" style={{ width: 46 }} title="Sin fotos">—</span>
                                      )}
                                      <div>
                                        {v.marca} {v.modelo} {v.anio}
                                        {v.color ? ` · ${v.color}` : ''}
                                      </div>
                                    </div>
                                  </td>
                                  <td>
                                    <EstadoBadge activo={v.activo} textoActivo="Activo" textoInactivo="Desactivado" />
                                  </td>
                                  <td>
                                    <div className="d-flex gap-1 justify-content-end">
                                      {v.activo && P.vEditar && (
                                        <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => abrirVehiculo(v)}>
                                          Editar
                                        </button>
                                      )}
                                      {P.vDesactivar && v.activo && (
                                        <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => abrirDesactivar('vehiculo', v.id, `Se desactivará el vehículo con placa ${v.placa}.`)}>
                                          Desactivar
                                        </button>
                                      )}
                                      {P.vDesactivar && !v.activo && cliente.activo && (
                                        <button type="button" className="btn btn-sm btn-outline-success" onClick={() => reactivar('vehiculo', v.id)}>
                                          Reactivar
                                        </button>
                                      )}
                                    </div>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <Modal
        abierto={modalCliente}
        grande
        titulo={editandoId ? `Corregir datos de ${cliente ? cliente.nombreCompleto : ''}` : 'Nuevo cliente'}
        onCerrar={() => setModalCliente(false)}
        onSubmit={guardarCliente}
        pie={
          <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoGuardar} textoCargando="Guardando...">
            Registrar cliente
          </BotonCargando>
        }
      >
        {modalCl.alerta && (
          <Alerta tipo={modalCl.alerta.tipo} mensaje={modalCl.alerta.mensaje} detalles={modalCl.alerta.detalles} onCerrar={modalCl.limpiar} />
        )}
        <div className="row g-3">
          <div className="col-md-4">
            <Campo label="Nombre(s)" required htmlFor="cNombre" error={errores.nombre}>
              <input id="cNombre" name="nombre" className={`form-control${errores.nombre ? ' is-invalid' : ''}`} required maxLength="80" autoComplete="off" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Apellido paterno" required htmlFor="cApellidoPaterno" error={errores.apellidoPaterno}>
              <input id="cApellidoPaterno" name="apellidoPaterno" className={`form-control${errores.apellidoPaterno ? ' is-invalid' : ''}`} required maxLength="80" autoComplete="off" value={form.apellidoPaterno} onChange={(e) => setForm({ ...form, apellidoPaterno: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Apellido materno" htmlFor="cApellidoMaterno" error={errores.apellidoMaterno}>
              <input id="cApellidoMaterno" name="apellidoMaterno" className={`form-control${errores.apellidoMaterno ? ' is-invalid' : ''}`} maxLength="80" autoComplete="off" value={form.apellidoMaterno} onChange={(e) => setForm({ ...form, apellidoMaterno: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono (10 dígitos)" required htmlFor="cTelefono" error={errores.telefono}>
              <input id="cTelefono" name="telefono" type="tel" inputMode="tel" placeholder="Ej. 55 1234 5678" className={`form-control${errores.telefono ? ' is-invalid' : ''}`} required maxLength="20" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono alterno" htmlFor="cTelefonoAlterno" error={errores.telefonoAlterno}>
              <input id="cTelefonoAlterno" name="telefonoAlterno" type="tel" inputMode="tel" className={`form-control${errores.telefonoAlterno ? ' is-invalid' : ''}`} maxLength="20" value={form.telefonoAlterno} onChange={(e) => setForm({ ...form, telefonoAlterno: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono personal" htmlFor="cTelefonoPersonal" error={errores.telefonoPersonal}>
              <input id="cTelefonoPersonal" name="telefonoPersonal" type="tel" inputMode="tel" className={`form-control${errores.telefonoPersonal ? ' is-invalid' : ''}`} maxLength="20" value={form.telefonoPersonal} onChange={(e) => setForm({ ...form, telefonoPersonal: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono de trabajo" htmlFor="cTelefonoTrabajo" error={errores.telefonoTrabajo}>
              <input id="cTelefonoTrabajo" name="telefonoTrabajo" type="tel" inputMode="tel" className={`form-control${errores.telefonoTrabajo ? ' is-invalid' : ''}`} maxLength="20" value={form.telefonoTrabajo} onChange={(e) => setForm({ ...form, telefonoTrabajo: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Contacto alternativo (nombre completo)" htmlFor="cNombreContacto" error={errores.nombreContacto}>
              <input id="cNombreContacto" name="nombreContacto" placeholder="Ej. María López" className={`form-control${errores.nombreContacto ? ' is-invalid' : ''}`} maxLength="160" autoComplete="off" value={form.nombreContacto} onChange={(e) => setForm({ ...form, nombreContacto: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono del contacto" htmlFor="cTelefonoContacto" error={errores.telefonoContacto}>
              <input id="cTelefonoContacto" name="telefonoContacto" type="tel" inputMode="tel" className={`form-control${errores.telefonoContacto ? ' is-invalid' : ''}`} maxLength="20" value={form.telefonoContacto} onChange={(e) => setForm({ ...form, telefonoContacto: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Correo electrónico" required htmlFor="cEmail" error={errores.email}>
              <input id="cEmail" name="email" type="email" className={`form-control${errores.email ? ' is-invalid' : ''}`} required maxLength="190" autoComplete="off" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="RFC" htmlFor="cRfc" error={errores.rfc}>
              <input id="cRfc" name="rfc" className={`form-control text-uppercase${errores.rfc ? ' is-invalid' : ''}`} maxLength="13" autoComplete="off" value={form.rfc} onChange={(e) => setForm({ ...form, rfc: e.target.value })} />
            </Campo>
          </div>
          {!editandoId && (
            <div className="col-12">
              <Campo label="Taller o talleres" required htmlFor="cTalleres" error={errores.tallerIds}>
                <div id="cTalleres" ref={comboRef} className="position-relative">
                  <button
                    type="button"
                    aria-expanded={comboTalleres}
                    onClick={() => setComboTalleres((o) => !o)}
                    className={`form-select text-start d-flex justify-content-between align-items-center${errores.tallerIds ? ' is-invalid border-danger' : ''}`}
                  >
                    <span className={tallerIds.length ? '' : 'text-muted'}>
                      {tallerIds.length ? `${tallerIds.length} taller(es) seleccionado(s)` : 'Selecciona talleres...'}
                    </span>
                    <span aria-hidden="true">▾</span>
                  </button>
                  {comboTalleres && (
                    <div className="border rounded shadow bg-white position-absolute w-100 mt-1" style={{ zIndex: 20, maxHeight: '260px', overflowY: 'auto' }}>
                      <div className="p-2 border-bottom sticky-top bg-white">
                        <input
                          autoFocus
                          placeholder="Buscar taller..."
                          aria-label="Buscar taller"
                          className="form-control form-control-sm"
                          value={buscaTaller}
                          onChange={(e) => setBuscaTaller(e.target.value)}
                        />
                      </div>
                      {talleresAlta.length === 0 && (
                        <div className="small text-muted px-3 py-2">No hay talleres activos disponibles.</div>
                      )}
                      {talleresFiltrados.length === 0 && talleresAlta.length > 0 && (
                        <div className="small text-muted px-3 py-2">Sin coincidencias.</div>
                      )}
                      {talleresFiltrados.map((t) => {
                        const idStr = String(t.id);
                        const marcado = tallerIds.includes(idStr);
                        return (
                          <label
                            key={t.id}
                            htmlFor={`taller-${t.id}`}
                            className="d-flex align-items-center gap-2 px-3 py-2 mb-0 border-bottom"
                            style={{ cursor: 'pointer', background: marcado ? 'var(--accent-soft)' : 'transparent' }}
                          >
                            <input
                              className="form-check-input mt-0" type="checkbox" id={`taller-${t.id}`}
                              checked={marcado}
                              onChange={() => alternarTaller(idStr)}
                            />
                            <span className="small">{t.nombre}</span>
                          </label>
                        );
                      })}
                    </div>
                  )}
                  {tallerIds.length > 0 && (
                    <div className="d-flex flex-wrap gap-1 mt-2">
                      {tallerIds.map((id) => {
                        const t = talleresAlta.find((x) => String(x.id) === id);
                        if (!t) return null;
                        return (
                          <span key={id} className="badge text-bg-primary d-inline-flex align-items-center gap-1">
                            {t.nombre}
                            <button
                              type="button" className="btn-close btn-close-white" style={{ fontSize: '0.6rem' }}
                              aria-label={`Quitar ${t.nombre}`}
                              onClick={() => alternarTaller(id)}
                            />
                          </span>
                        );
                      })}
                    </div>
                  )}
                </div>
                <div className="form-text">
                  {tallerIds.length === 0
                    ? 'Abre la lista, busca y marca al menos un taller.'
                    : `${tallerIds.length} taller(es) seleccionado(s).`} Si el correo ya existe, el registro se bloquea para evitar duplicados.
                </div>
              </Campo>
            </div>
          )}
          <div className="col-12">
            <h3 className="h6 fw-semibold mt-2 mb-0">Domicilio del cliente</h3>
          </div>
          <div className="col-md-8">
            <Campo label="Calle" required htmlFor="cCalle" error={errores.calle}>
              <input id="cCalle" name="calle" className={`form-control${errores.calle ? ' is-invalid' : ''}`} required maxLength="160" autoComplete="off" value={form.calle} onChange={(e) => setForm({ ...form, calle: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Número" required htmlFor="cNumero" error={errores.numero}>
              <input id="cNumero" name="numero" className={`form-control${errores.numero ? ' is-invalid' : ''}`} required maxLength="40" autoComplete="off" value={form.numero} onChange={(e) => setForm({ ...form, numero: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Código postal" required htmlFor="cCp" error={errores.codigoPostal}>
              <input id="cCp" name="codigoPostal" inputMode="numeric" maxLength="5" placeholder="5 dígitos" className={`form-control${errores.codigoPostal ? ' is-invalid' : ''}`} required value={form.codigoPostal} onChange={setF('codigoPostal')} />
              <div className="form-text">Escribe 5 dígitos y el estado, municipio y colonia se cargan solos.</div>
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Estado" required htmlFor="cEstado" error={errores.estado}>
              <select id="cEstado" name="estado" className={`form-select${errores.estado ? ' is-invalid' : ''}`} required value={form.estado} onChange={setF('estado')}>
                <option value="">Selecciona un estado</option>
                {estados.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Municipio" required htmlFor="cMunicipio" error={errores.municipio}>
              <select id="cMunicipio" name="municipio" className={`form-select${errores.municipio ? ' is-invalid' : ''}`} required disabled={!municipios.length} value={form.municipio} onChange={setF('municipio')}>
                <option value="">{municipios.length ? 'Selecciona un municipio' : 'Selecciona un estado primero'}</option>
                {municipios.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          <div className="col-md-8">
            <Campo label="Colonia" required htmlFor="cColonia" error={errores.colonia}>
              <select id="cColonia" name="colonia" className={`form-select${errores.colonia ? ' is-invalid' : ''}`} required disabled={!colonias.length} value={form.colonia} onChange={setF('colonia')}>
                <option value="">{colonias.length ? 'Selecciona una colonia' : 'Selecciona un municipio primero'}</option>
                {colonias.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Referencia adicional" htmlFor="cDireccion" error={errores.direccion}>
              <input id="cDireccion" name="direccion" placeholder="Ej. Casa azul, portón negro" className={`form-control${errores.direccion ? ' is-invalid' : ''}`} maxLength="250" value={form.direccion} onChange={(e) => setForm({ ...form, direccion: e.target.value })} />
            </Campo>
          </div>
          <div className="col-12">
            <div className="form-text" aria-live="polite">
              {avisoCp}
            </div>
          </div>
        </div>
      </Modal>

      <Modal
        abierto={modalVeh}
        grande
        titulo={vehEditId ? `Corregir vehículo ${formV.placa}` : 'Nuevo vehículo'}
        onCerrar={() => setModalVeh(false)}
        onSubmit={guardarVehiculo}
        pie={
          <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoV} textoCargando="Guardando...">
            Guardar vehículo
          </BotonCargando>
        }
      >
        {modalVh.alerta && (
          <Alerta tipo={modalVh.alerta.tipo} mensaje={modalVh.alerta.mensaje} detalles={modalVh.alerta.detalles} onCerrar={modalVh.limpiar} />
        )}
        <p className="small text-muted mb-3">
          Propietario: <span className="fw-semibold">{cliente ? `${cliente.nombreCompleto} (#${cliente.id})` : ''}</span>
        </p>
        <ComboTaller opciones={talleresVeh} valor={formV.tallerId}
          onChange={(id) => setFormV((actual) => ({ ...actual, tallerId: id }))}
          etiqueta="Taller asignado al vehículo *" error={erroresV.tallerId} />
        <div className="row g-3">
          <div className="col-md-4">
            <Campo label="Placa" required htmlFor="vPlaca" error={erroresV.placa}>
              <input id="vPlaca" name="placa" placeholder="Ej. ABC-123" className={`form-control text-uppercase${erroresV.placa ? ' is-invalid' : ''}`} required maxLength="14" autoComplete="off" value={formV.placa} onChange={(e) => setFormV({ ...formV, placa: e.target.value.toUpperCase() })} />
              <div className="small text-muted mt-1">Mayúsculas separadas por guion (ej. ABC-123)</div>
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Marca" required htmlFor="vMarca" error={erroresV.marca}>
              <input id="vMarca" name="marca" placeholder="Ej. Nissan" className={`form-control${erroresV.marca ? ' is-invalid' : ''}`} required maxLength="40" autoComplete="off" value={formV.marca} onChange={(e) => setFormV({ ...formV, marca: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Modelo" required htmlFor="vModelo" error={erroresV.modelo}>
              <input id="vModelo" name="modelo" placeholder="Ej. Versa" className={`form-control${erroresV.modelo ? ' is-invalid' : ''}`} required maxLength="60" autoComplete="off" value={formV.modelo} onChange={(e) => setFormV({ ...formV, modelo: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-3">
            <Campo label="Año" required htmlFor="vAnio" error={erroresV.anio}>
              <input id="vAnio" name="anio" inputMode="numeric" placeholder="Ej. 2022" className={`form-control${erroresV.anio ? ' is-invalid' : ''}`} required maxLength="4" value={formV.anio} onChange={(e) => setFormV({ ...formV, anio: e.target.value.replace(/[^0-9]/g, '') })} />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="VIN / Número de serie" htmlFor="vVin" error={erroresV.vin}>
              <input id="vVin" name="vin" className={`form-control text-uppercase${erroresV.vin ? ' is-invalid' : ''}`} maxLength="17" autoComplete="off" value={formV.vin} onChange={(e) => setFormV({ ...formV, vin: e.target.value.toUpperCase() })} />
            </Campo>
          </div>
          <div className="col-md-3">
            <Campo label="Color" htmlFor="vColor" error={erroresV.color}>
              <input id="vColor" name="color" className={`form-control${erroresV.color ? ' is-invalid' : ''}`} maxLength="30" value={formV.color} onChange={(e) => setFormV({ ...formV, color: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Versión" htmlFor="vVersion" error={erroresV.version}>
              <input id="vVersion" name="version" className={`form-control${erroresV.version ? ' is-invalid' : ''}`} maxLength="30" value={formV.version} onChange={(e) => setFormV({ ...formV, version: e.target.value })} />
            </Campo>
          </div>
          <div className="col-md-4">
            <Campo label="Kilometraje" required htmlFor="vKilometraje" error={erroresV.kilometraje}>
              <input id="vKilometraje" name="kilometraje" type="number" min="0" step="1" inputMode="numeric" className={`form-control${erroresV.kilometraje ? ' is-invalid' : ''}`} required value={formV.kilometraje} onChange={(e) => setFormV({ ...formV, kilometraje: e.target.value })} />
            </Campo>
          </div>
          <div className="col-12">
            <label className="form-label" htmlFor="vFotos">Fotos del vehículo</label>
            <input
              id="vFotos" name="fotos" type="file"
              className={`form-control${erroresV.fotos || errorFotosV ? ' is-invalid' : ''}`}
              accept="image/jpeg,image/png,image/webp" multiple onChange={subirFotosV} disabled={subiendoFotosV}
            />
            <div className="form-text">Solo fotos JPG, PNG o WebP (nada de documentos). Hasta {MAX_FOTOS_VEH} fotos, máximo 15 MB cada una.</div>
            {(erroresV.fotos || errorFotosV) && <div className="invalid-feedback d-block">{errorFotosV || erroresV.fotos}</div>}
            {subiendoFotosV && <div className="small text-muted mt-1">Subiendo foto...</div>}
            {fotosActualesV.length > 0 && (
              <div className="d-flex flex-wrap gap-2 mt-2">
                {fotosActualesV.map((ruta) => (
                  <div key={ruta} className="position-relative">
                    <img
                      src={ruta} alt="Foto del vehículo"
                      style={{ width: 104, height: 78, objectFit: 'cover', borderRadius: 6, border: '1px solid #dee2e6' }}
                    />
                    <button
                      type="button" className="btn btn-danger position-absolute top-0 end-0"
                      style={{ padding: '0 6px', lineHeight: 1.2, fontSize: 12 }}
                      onClick={() => quitarFotoV(ruta)} title="Quitar foto"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </Modal>

      <Modal
        abierto={modalDes}
        titulo={objetivo && objetivo.tipo === 'cliente' ? 'Desactivar cliente' : 'Desactivar vehículo'}
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
        <p className="small">{descripcionDesactivar}</p>
        <Campo label="Motivo" required htmlFor="dMotivo" error={errorMotivo}>
          <textarea id="dMotivo" name="motivo" className={`form-control${errorMotivo ? ' is-invalid' : ''}`} rows="3" required minLength="5" maxLength="250" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
        </Campo>
      </Modal>
      {zona}
    </Layout>
  );
}

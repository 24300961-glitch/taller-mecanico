import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, fechaCortaFecha, RX, texto } from '../api.js';
import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';
import { Alerta, BotonCargando, Campo, EstadoBadge, Modal, useAlerta, useToasts } from '../components/ui.jsx';

const VACIO = { email: '', fullName: '', password: '', confirmPassword: '', recoveryPhone: '', rolId: '' };

function validarAlta(d) {
  const e = {};
  const email = d.email.trim();
  if (!email) e.email = 'El correo es obligatorio.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) e.email = 'Escribe un correo válido.';
  const nombre = d.fullName.trim();
  if (!nombre) e.fullName = 'El nombre es obligatorio.';
  else if (nombre.length < 3 || nombre.length > 100) e.fullName = 'El nombre debe tener entre 3 y 100 caracteres.';
  else if (!RX.NOMBRE_100.test(nombre)) e.fullName = 'El nombre contiene caracteres no permitidos.';
  if (!d.password) e.password = 'La contraseña es obligatoria.';
  else if (d.password.length < 12 || d.password.length > 72) e.password = 'La contraseña debe tener entre 12 y 72 caracteres.';
  else if (!/[A-Z]/.test(d.password) || !/[a-z]/.test(d.password) || !/[0-9]/.test(d.password) || !/[^A-Za-z0-9\s]/.test(d.password))
    e.password = 'Incluye mayúscula, minúscula, número y símbolo.';
  if (d.password !== d.confirmPassword) e.confirmPassword = 'La confirmación no coincide.';
  if (d.recoveryPhone.trim() && !/^\+[1-9]\d{7,14}$/.test(d.recoveryPhone.trim()))
    e.recoveryPhone = 'Usa formato internacional E.164 (ej. +5215512345678).';
  if (!d.rolId) e.rolId = 'Selecciona un rol.';
  return e;
}

function validarEdicion(d) {
  const e = {};
  const nombre = d.fullName.trim();
  if (!nombre) e.fullName = 'El nombre es obligatorio.';
  else if (nombre.length < 3 || nombre.length > 100) e.fullName = 'El nombre debe tener entre 3 y 100 caracteres.';
  else if (!RX.NOMBRE_100.test(nombre)) e.fullName = 'El nombre contiene caracteres no permitidos.';
  const email = d.email.trim();
  if (!email) e.email = 'El correo es obligatorio.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) e.email = 'Escribe un correo válido.';
  return e;
}

export default function Usuarios() {
  const { me, tienePermiso } = useAuth();
  const puedeEditar = tienePermiso('USUARIOS_EDITAR') || tienePermiso('ROLES_ASIGNAR');
  const puedeSuspender = tienePermiso('USUARIOS_SUSPENDER') || tienePermiso('ROLES_ASIGNAR');
  const puedeCrear = tienePermiso('ROLES_ASIGNAR');
  const esAdmin = tienePermiso('ROLES_ASIGNAR');
  const puedeRoles = tienePermiso('ROLES_CREAR') || tienePermiso('ROLES_ASIGNAR');
  const verAcciones = puedeEditar || puedeSuspender;
  const [usuarios, setUsuarios] = useState(null);
  const [roles, setRoles] = useState([]);
  const [error, setError] = useState('');
  const [ocupadoId, setOcupadoId] = useState(null);
  const [altaAbierta, setAltaAbierta] = useState(false);
  const [ocupadoAlta, setOcupadoAlta] = useState(false);
  const [form, setForm] = useState(VACIO);
  const [erroresAlta, setErroresAlta] = useState({});
  const [editando, setEditando] = useState(null);
  const [formEdit, setFormEdit] = useState({ fullName: '', email: '' });
  const [erroresEdit, setErroresEdit] = useState({});
  const [ocupadoEdit, setOcupadoEdit] = useState(false);
  const general = useAlerta();
  const edicion = useAlerta();
  const { push, zona } = useToasts();

  const cargar = async () => {
    try {
      const lista = await api('/api/admin/usuarios');
      setUsuarios(lista || []);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    cargar();
    if (puedeCrear) {
      api('/api/admin/roles')
        .then((lista) => setRoles(lista || []))
        .catch(() => setRoles([]));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const abrirAlta = () => {
    setForm(VACIO);
    setErroresAlta({});
    general.limpiar();
    setAltaAbierta(true);
  };

  const abrirEdicion = (u) => {
    setEditando(u);
    setFormEdit({ fullName: u.fullName || '', email: u.email || '' });
    setErroresEdit({});
    edicion.limpiar();
  };

  const setF = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const setFE = (k) => (e) => setFormEdit((f) => ({ ...f, [k]: e.target.value }));

  const enviarAlta = async (ev) => {
    ev.preventDefault();
    const e = validarAlta(form);
    setErroresAlta(e);
    if (Object.keys(e).length > 0) {
      general.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    setOcupadoAlta(true);
    general.limpiar();
    try {
      const r = await api('/api/admin/usuarios', {
        method: 'POST',
        body: {
          email: form.email.trim(),
          fullName: form.fullName.trim(),
          password: form.password,
          confirmPassword: form.confirmPassword,
          recoveryPhone: form.recoveryPhone.trim(),
          rolId: Number(form.rolId),
        },
      });
      setAltaAbierta(false);
      setForm(VACIO);
      push(`Usuario ${r.fullName} creado correctamente`, true);
      await cargar();
    } catch (err) {
      general.mostrarError(err);
      if (err.data && err.data.errores) setErroresAlta(err.data.errores);
    } finally {
      setOcupadoAlta(false);
    }
  };

  const enviarEdicion = async (ev) => {
    ev.preventDefault();
    const e = validarEdicion(formEdit);
    setErroresEdit(e);
    if (Object.keys(e).length > 0) {
      edicion.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    setOcupadoEdit(true);
    edicion.limpiar();
    try {
      const r = await api(`/api/admin/usuarios/${editando.id}`, {
        method: 'PATCH',
        body: { fullName: formEdit.fullName.trim(), email: formEdit.email.trim() },
      });
      setUsuarios((lista) => (lista || []).map((x) => (x.id === editando.id ? { ...x, fullName: r.fullName, email: r.email } : x)));
      setEditando(null);
      push(r.message || 'Datos actualizados', true);
    } catch (err) {
      edicion.mostrarError(err);
      if (err.data && err.data.errores) setErroresEdit(err.data.errores);
    } finally {
      setOcupadoEdit(false);
    }
  };

  const cambiarEstado = async (u) => {
    const suspender = u.enabled;
    const pregunta = suspender
      ? `¿Suspender la cuenta de ${u.fullName}? No podrá iniciar sesión y se cerrarán sus sesiones abiertas.`
      : `¿Reactivar la cuenta de ${u.fullName}? Podrá iniciar sesión de nuevo.`;
    if (!window.confirm(pregunta)) return;
    setOcupadoId(u.id);
    try {
      const r = await api(`/api/admin/usuarios/${u.id}/${suspender ? 'suspender' : 'reactivar'}`, { method: 'PATCH' });
      setUsuarios((lista) => (lista || []).map((x) => (x.id === u.id ? { ...x, enabled: !suspender } : x)));
      push(r.message || 'Estado actualizado', true);
    } catch (e) {
      push(e.message, false);
    } finally {
      setOcupadoId(null);
    }
  };

  const rolesAsignables = roles.filter((r) => r.asignable && r.activo);

  return (
    <Layout titulo="Usuarios" subtitulo="Cuentas registradas en el sistema">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <div>
          <h2 className="h5 fw-bold mb-1">Cuentas registradas</h2>
          <p className="text-muted mb-0 small">Consulta, alta, edición y suspensión de usuarios y el rol que tienen asignado.</p>
        </div>
        <div className="d-flex gap-2">
          {puedeCrear && (
            <button type="button" className="btn btn-accent" onClick={abrirAlta}>
              + Nuevo usuario
            </button>
          )}
          {puedeRoles && (
            <Link to="/roles" className="btn btn-outline-secondary">
              Ir a Asignar roles
            </Link>
          )}
        </div>
      </div>
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      <div className="data-card shadow-sm">
        <div className="card-header">
          <h3 className="h6 fw-bold mb-0">Total de cuentas: {usuarios ? usuarios.length : 0}</h3>
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Correo</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Último acceso</th>
                <th>Registro</th>
                {verAcciones && <th className="text-end">Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {usuarios === null && !error && (
                <tr>
                  <td colSpan={verAcciones ? 7 : 6} className="empty-state">
                    Cargando usuarios...
                  </td>
                </tr>
              )}
              {usuarios !== null && usuarios.length === 0 && (
                <tr>
                  <td colSpan={verAcciones ? 7 : 6} className="empty-state">
                    Aún no hay usuarios registrados.
                  </td>
                </tr>
              )}
              {(usuarios || []).map((u) => (
                <tr key={u.id}>
                  <td className="fw-semibold small">{u.fullName}</td>
                  <td className="small">{u.email}</td>
                  <td>
                    <span className="role-badge">{u.rol || 'Sin rol'}</span>
                  </td>
                  <td>
                    <EstadoBadge activo={u.enabled} textoActivo="Activo" textoInactivo="Suspendido" />
                  </td>
                  <td className="small">{u.ultimoAcceso ? fechaCortaFecha(u.ultimoAcceso) : 'Nunca'}</td>
                  <td className="small">{u.registro ? fechaCortaFecha(u.registro) : texto(null)}</td>
                  {verAcciones && (
                    <td className="text-end">
                      <div className="d-flex justify-content-end gap-2">
                        {u.administrativo && !esAdmin ? (
                          <span className="text-muted small align-self-center">Solo Admin</span>
                        ) : (
                          <>
                            {puedeEditar && (
                              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => abrirEdicion(u)}>
                                Editar
                              </button>
                            )}
                            {puedeSuspender && (me && u.email === me.email ? (
                              <span className="text-muted small align-self-center">Tu cuenta</span>
                            ) : (
                              <BotonCargando
                                type="button"
                                className={`btn btn-sm ${u.enabled ? 'btn-outline-warning' : 'btn-outline-success'}`}
                                cargando={ocupadoId === u.id}
                                textoCargando="Guardando..."
                                onClick={() => cambiarEstado(u)}
                              >
                                {u.enabled ? 'Suspender' : 'Reactivar'}
                              </BotonCargando>
                            ))}
                          </>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        abierto={altaAbierta}
        titulo="Nuevo usuario"
        onCerrar={() => setAltaAbierta(false)}
        onSubmit={enviarAlta}
        grande
        pie={
          <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoAlta} textoCargando="Creando...">
            Crear usuario
          </BotonCargando>
        }
      >
        {general.alerta && (
          <Alerta
            tipo={general.alerta.tipo}
            mensaje={general.alerta.mensaje}
            detalles={general.alerta.detalles}
            onCerrar={general.limpiar}
          />
        )}
        <div className="row">
          <div className="col-md-6">
            <Campo label="Correo" required htmlFor="nuEmail" error={erroresAlta.email}>
              <input
                id="nuEmail" type="email" className={`form-control${erroresAlta.email ? ' is-invalid' : ''}`}
                value={form.email} onChange={setF('email')} autoComplete="off" maxLength="190"
              />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Nombre completo" required htmlFor="nuNombre" error={erroresAlta.fullName}>
              <input
                id="nuNombre" type="text" className={`form-control${erroresAlta.fullName ? ' is-invalid' : ''}`}
                value={form.fullName} onChange={setF('fullName')} autoComplete="off" maxLength="100"
              />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Contraseña" required htmlFor="nuPassword" error={erroresAlta.password}>
              <input
                id="nuPassword" type="password" className={`form-control${erroresAlta.password ? ' is-invalid' : ''}`}
                value={form.password} onChange={setF('password')} autoComplete="new-password" maxLength="72"
              />
              <div className="form-text">Entre 12 y 72 caracteres, con mayúscula, minúscula, número y símbolo.</div>
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Confirmar contraseña" required htmlFor="nuConfirm" error={erroresAlta.confirmPassword}>
              <input
                id="nuConfirm" type="password" className={`form-control${erroresAlta.confirmPassword ? ' is-invalid' : ''}`}
                value={form.confirmPassword} onChange={setF('confirmPassword')} autoComplete="new-password" maxLength="72"
              />
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Teléfono de recuperación" htmlFor="nuTelefono" error={erroresAlta.recoveryPhone}>
              <input
                id="nuTelefono" type="tel" className={`form-control${erroresAlta.recoveryPhone ? ' is-invalid' : ''}`}
                value={form.recoveryPhone} onChange={setF('recoveryPhone')} autoComplete="off" maxLength="16"
                placeholder="+5215512345678"
              />
              <div className="form-text">Opcional. Formato internacional E.164.</div>
            </Campo>
          </div>
          <div className="col-md-6">
            <Campo label="Rol" required htmlFor="nuRol" error={erroresAlta.rolId}>
              <select
                id="nuRol" className={`form-select${erroresAlta.rolId ? ' is-invalid' : ''}`}
                value={form.rolId} onChange={setF('rolId')}
              >
                <option value="">Selecciona un rol</option>
                {rolesAsignables.map((r) => (
                  <option key={r.id} value={r.id}>{r.nombre}</option>
                ))}
              </select>
            </Campo>
          </div>
        </div>
      </Modal>

      <Modal
        abierto={!!editando}
        titulo="Editar usuario"
        onCerrar={() => setEditando(null)}
        onSubmit={enviarEdicion}
        pie={
          <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoEdit} textoCargando="Guardando...">
            Guardar cambios
          </BotonCargando>
        }
      >
        {edicion.alerta && (
          <Alerta
            tipo={edicion.alerta.tipo}
            mensaje={edicion.alerta.mensaje}
            detalles={edicion.alerta.detalles}
            onCerrar={edicion.limpiar}
          />
        )}
        <Campo label="Nombre completo" required htmlFor="edNombre" error={erroresEdit.fullName}>
          <input
            id="edNombre" type="text" className={`form-control${erroresEdit.fullName ? ' is-invalid' : ''}`}
            value={formEdit.fullName} onChange={setFE('fullName')} autoComplete="off" maxLength="100"
          />
        </Campo>
        <Campo label="Correo" required htmlFor="edEmail" error={erroresEdit.email}>
          <input
            id="edEmail" type="email" className={`form-control${erroresEdit.email ? ' is-invalid' : ''}`}
            value={formEdit.email} onChange={setFE('email')} autoComplete="off" maxLength="190"
          />
        </Campo>
        <div className="form-text">
          Si cambias el correo, el usuario deberá iniciar sesión de nuevo. El rol se cambia desde
          {' '}<Link to="/roles">Asignar roles</Link>.
        </div>
      </Modal>
      {zona}
    </Layout>
  );
}

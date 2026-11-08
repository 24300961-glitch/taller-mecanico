import { useEffect, useState } from 'react';
import { api, claveNormalizada, RX } from '../api.js';
import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';
import { Alerta, BotonCargando, Campo, EstadoBadge, useAlerta, useToasts } from '../components/ui.jsx';

export default function Roles() {
  const { me, tienePermiso } = useAuth();
  const puedeCrear = tienePermiso('ROLES_CREAR');
  const puedeAsignar = tienePermiso('ROLES_ASIGNAR');
  const [roles, setRoles] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [nombreRol, setNombreRol] = useState('');
  const [errorRol, setErrorRol] = useState('');
  const [rolEditandoId, setRolEditandoId] = useState(null);
  const [ocupadoCrear, setOcupadoCrear] = useState(false);
  const [usuarioId, setUsuarioId] = useState('');
  const [rolId, setRolId] = useState('');
  const [errorAsignar, setErrorAsignar] = useState('');
  const [rolActual, setRolActual] = useState('');
  const [ocupadoAsignar, setOcupadoAsignar] = useState(false);
  const general = useAlerta();
  const crear = useAlerta();
  const asignar = useAlerta();
  const { push, zona } = useToasts();

  const cargarRoles = async () => {
    try {
      const lista = await api('/api/admin/roles');
      setRoles(lista || []);
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const cargarUsuarios = async () => {
    try {
      const lista = await api('/api/admin/usuarios');
      setUsuarios(lista || []);
    } catch (e) {
      general.mostrarError(e);
    }
  };

  useEffect(() => {
    cargarRoles();
    if (puedeAsignar) cargarUsuarios();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const modoAltaRol = () => {
    setRolEditandoId(null);
    setNombreRol('');
    setErrorRol('');
    crear.limpiar();
  };

  const abrirEdicionRol = (r) => {
    setRolEditandoId(r.id);
    setNombreRol(r.nombre);
    setErrorRol('');
    crear.limpiar();
    document.getElementById('componenteCrearRol')?.scrollIntoView({ behavior: 'smooth' });
  };

  const enviarRol = async (ev) => {
    ev.preventDefault();
    crear.limpiar();
    const v = nombreRol.trim();
    if (!v) {
      setErrorRol('El nombre del rol es obligatorio y debe tener entre 3 y 60 caracteres');
      crear.mostrar('danger', 'Corrige el campo marcado antes de continuar.');
      return;
    }
    if (v.length < 3 || v.length > 60) {
      setErrorRol('El nombre del rol es obligatorio y debe tener entre 3 y 60 caracteres');
      crear.mostrar('danger', 'Corrige el campo marcado antes de continuar.');
      return;
    }
    if (!RX.ROL.test(v)) {
      setErrorRol('El nombre del rol solo puede contener letras, números, espacios, guiones y guiones bajos');
      crear.mostrar('danger', 'Corrige el campo marcado antes de continuar.');
      return;
    }
    if (v && roles.some((r) => r.id !== rolEditandoId && claveNormalizada(r.nombre) === claveNormalizada(v))) {
      setErrorRol('Ya existe un rol con ese nombre');
      crear.mostrar('danger', 'Corrige el campo marcado antes de continuar.');
      return;
    }
    setOcupadoCrear(true);
    try {
      const url = rolEditandoId ? `/api/admin/roles/${rolEditandoId}` : '/api/admin/roles';
      const r = await api(url, { method: rolEditandoId ? 'PUT' : 'POST', body: { nombre: v } });
      crear.mostrar('success', r.message);
      push(r.message, true);
      modoAltaRol();
      await cargarRoles();
    } catch (e) {
      crear.mostrarError(e);
      if (e.data && e.data.errores && e.data.errores.nombre) setErrorRol(e.data.errores.nombre);
      else if (e.status === 409) setErrorRol(e.message);
    } finally {
      setOcupadoCrear(false);
    }
  };

  const suspenderRol = async (r) => {
    try {
      const s = await api(`/api/admin/roles/${r.id}/suspender`, { method: 'PATCH' });
      general.mostrar('success', s.message);
      push(s.message, true);
      await cargarRoles();
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const reactivarRol = async (r) => {
    try {
      const s = await api(`/api/admin/roles/${r.id}/reactivar`, { method: 'PATCH' });
      general.mostrar('success', s.message);
      push(s.message, true);
      await cargarRoles();
    } catch (e) {
      general.mostrarError(e);
    }
  };

  const alCambiarUsuario = (v) => {
    setUsuarioId(v);
    const u = usuarios.find((x) => String(x.id) === v);
    setRolActual(u ? `Rol actual: ${u.rol || 'Sin rol'}` : '');
  };

  const enviarAsignacion = async (ev) => {
    ev.preventDefault();
    asignar.limpiar();
    if (!usuarioId || !rolId) {
      setErrorAsignar(!usuarioId ? 'Selecciona un usuario' : 'Selecciona un rol');
      asignar.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    const u = usuarios.find((x) => String(x.id) === String(usuarioId));
    if (u && String(u.rolId) === String(rolId)) {
      setErrorAsignar('El usuario ya tiene este rol');
      asignar.mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    setOcupadoAsignar(true);
    try {
      const r = await api(`/api/admin/usuarios/${usuarioId}/rol`, { method: 'PATCH', body: { rolId: Number(rolId) } });
      asignar.mostrar('success', r.message);
      push('Rol asignado correctamente', true);
      setRolId('');
      await cargarUsuarios();
    } catch (e) {
      asignar.mostrarError(e);
      if (e.data && e.data.errores && e.data.errores.rolId) setErrorAsignar(e.data.errores.rolId);
    } finally {
      setOcupadoAsignar(false);
    }
  };

  const rolesAsignables = roles.filter((r) => r.asignable && r.activo);

  return (
    <Layout titulo="Asignar roles" subtitulo="Creación de roles y asignación a usuarios">
      {general.alerta && (
        <Alerta tipo={general.alerta.tipo} mensaje={general.alerta.mensaje} detalles={general.alerta.detalles} onCerrar={general.limpiar} />
      )}
      <div className="row g-4">
        <div className="col-xl-5">
          {puedeCrear ? (
            <div className="data-card shadow-sm mb-4" id="componenteCrearRol">
              <div className="card-header d-flex justify-content-between align-items-center">
                <h2 className="fw-semibold mb-0 section-h5">{rolEditandoId ? 'Editar rol' : 'Crear rol'}</h2>
                {rolEditandoId && (
                  <button className="btn btn-sm btn-outline-secondary" type="button" onClick={modoAltaRol}>
                    Cancelar edición
                  </button>
                )}
              </div>
              <div className="p-3 p-md-4">
                {crear.alerta && (
                  <Alerta tipo={crear.alerta.tipo} mensaje={crear.alerta.mensaje} detalles={crear.alerta.detalles} onCerrar={crear.limpiar} />
                )}
                <form noValidate onSubmit={enviarRol}>
                  <Campo label="Nombre del rol" required htmlFor="nombreRol" error={errorRol}>
                    <input
                      type="text" id="nombreRol" name="nombre" className={`form-control${errorRol ? ' is-invalid' : ''}`}
                      required minLength="3" maxLength="60" autoComplete="off" placeholder="Ej. Jefe de taller"
                      value={nombreRol} onChange={(e) => setNombreRol(e.target.value)}
                    />
                    <div className="form-text">Letras, números, espacios, guiones y guiones bajos. No se permiten nombres repetidos.</div>
                  </Campo>
                  <BotonCargando type="submit" className="btn btn-accent" cargando={ocupadoCrear} textoCargando={rolEditandoId ? 'Guardando...' : 'Creando...'}>
                    {rolEditandoId ? 'Guardar cambios' : 'Crear rol'}
                  </BotonCargando>
                </form>
              </div>
            </div>
          ) : (
            <div className="alert alert-info">Solo el Administrador puede crear roles.</div>
          )}
        </div>
        <div className="col-xl-7">
          {puedeAsignar && (
            <>
              <div className="data-card shadow-sm mb-4">
                <div className="card-header">
                  <h2 className="fw-semibold mb-0 section-h5">Asignar rol</h2>
                </div>
                <div className="p-3 p-md-4">
                  {asignar.alerta && (
                    <Alerta tipo={asignar.alerta.tipo} mensaje={asignar.alerta.mensaje} detalles={asignar.alerta.detalles} onCerrar={asignar.limpiar} />
                  )}
                  <form noValidate onSubmit={enviarAsignacion}>
                    <div className="row g-3">
                      <div className="col-md-6">
                        <Campo label="Usuario" required htmlFor="usuarioId" error={!usuarioId && errorAsignar && !rolId ? errorAsignar : ''}>
                          <select
                            id="usuarioId" name="usuarioId" className="form-select" required
                            value={usuarioId} onChange={(e) => alCambiarUsuario(e.target.value)}
                          >
                            <option value="">Selecciona un usuario</option>
                            {usuarios.map((u) => (
                              <option key={u.id} value={u.id} disabled={me && u.email === me.email}>
                                {u.fullName} ({u.email}){me && u.email === me.email ? ' — tu cuenta' : ''}
                              </option>
                            ))}
                          </select>
                          <div className="form-text">{rolActual}</div>
                        </Campo>
                      </div>
                      <div className="col-md-6">
                        <Campo label="Rol a asignar" required htmlFor="rolId" error={errorAsignar}>
                          <select
                            id="rolId" name="rolId" className={`form-select${errorAsignar ? ' is-invalid' : ''}`} required
                            value={rolId} onChange={(e) => setRolId(e.target.value)}
                          >
                            <option value="">Selecciona un rol</option>
                            {rolesAsignables.map((r) => (
                              <option key={r.id} value={r.id}>
                                {r.nombre}
                              </option>
                            ))}
                          </select>
                        </Campo>
                      </div>
                    </div>
                    <BotonCargando type="submit" className="btn btn-accent mt-3" cargando={ocupadoAsignar} textoCargando="Asignando...">
                      Asignar rol
                    </BotonCargando>
                  </form>
                </div>
              </div>
              <div className="data-card shadow-sm">
                <div className="card-header d-flex justify-content-between align-items-center">
                  <h2 className="fw-semibold mb-0 section-h5">Usuarios y su rol</h2>
                  <span className="badge text-bg-light">{usuarios.length}</span>
                </div>
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th>Usuario</th>
                        <th>Correo</th>
                        <th>Rol</th>
                        <th>Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {usuarios.length === 0 && (
                        <tr>
                          <td colSpan="4" className="empty-state">
                            No hay usuarios registrados.
                          </td>
                        </tr>
                      )}
                      {usuarios.map((u) => (
                        <tr key={u.id}>
                          <td className="fw-semibold small">{u.fullName}</td>
                          <td className="small">{u.email}</td>
                          <td>
                            <span className="role-badge">{u.rol || 'Sin rol'}</span>
                          </td>
                          <td>
                            <EstadoBadge activo={u.enabled} textoActivo="Activo" textoInactivo="Desactivado" />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
      {puedeCrear && (
        <div className="data-card shadow-sm mt-4">
          <div className="card-header d-flex justify-content-between align-items-center">
            <h2 className="fw-semibold mb-0 section-h5">Roles registrados</h2>
            <span className="badge text-bg-light">{roles.length}</span>
          </div>
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Rol</th>
                  <th>Permisos</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {roles.length === 0 && (
                  <tr>
                    <td colSpan="4" className="empty-state">
                      No hay roles registrados.
                    </td>
                  </tr>
                )}
                {roles.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <div className="fw-semibold small">{r.nombre}</div>
                      {r.inicial && <div className="small text-muted">Rol del sistema</div>}
                    </td>
                    <td className="small text-muted">{(r.permisos || []).length} permiso(s)</td>
                    <td>
                      <EstadoBadge activo={r.activo} textoActivo="Activo" textoInactivo="Suspendido" />
                    </td>
                    <td className="text-end">
                      {!r.inicial && r.asignable ? (
                        <div className="d-flex gap-1 justify-content-end">
                          <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => abrirEdicionRol(r)}>
                            Editar
                          </button>
                          {r.activo ? (
                            <button type="button" className="btn btn-sm btn-outline-warning" onClick={() => suspenderRol(r)}>
                              Suspender
                            </button>
                          ) : (
                            <button type="button" className="btn btn-sm btn-outline-success" onClick={() => reactivarRol(r)}>
                              Reactivar
                            </button>
                          )}
                        </div>
                      ) : (
                        <span className="small text-muted">Protegido</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-3 py-2 small text-muted border-top">
            Los roles del sistema no se pueden renombrar ni suspender. Suspender conserva la fila en la base de datos y
            retira el rol de las asignaciones.
          </div>
        </div>
      )}
      {zona}
    </Layout>
  );
}

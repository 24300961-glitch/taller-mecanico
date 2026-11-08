import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, fechaCortaFecha, texto } from '../api.js';
import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';
import { EstadoBadge } from '../components/ui.jsx';

export default function Usuarios() {
  const { tienePermiso } = useAuth();
  const [usuarios, setUsuarios] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/admin/usuarios')
      .then(setUsuarios)
      .catch((e) => setError(e.message));
  }, []);

  const puedeRoles = tienePermiso('ROLES_CREAR') || tienePermiso('ROLES_ASIGNAR');

  return (
    <Layout titulo="Usuarios" subtitulo="Cuentas registradas en el sistema">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <div>
          <h2 className="h5 fw-bold mb-1">Cuentas registradas</h2>
          <p className="text-muted mb-0 small">Consulta de usuarios y el rol que tienen asignado.</p>
        </div>
        {puedeRoles && (
          <Link to="/roles" className="btn btn-accent">
            Ir a Asignar roles
          </Link>
        )}
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
              </tr>
            </thead>
            <tbody>
              {usuarios === null && !error && (
                <tr>
                  <td colSpan="6" className="empty-state">
                    Cargando usuarios...
                  </td>
                </tr>
              )}
              {usuarios !== null && usuarios.length === 0 && (
                <tr>
                  <td colSpan="6" className="empty-state">
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
                    <EstadoBadge activo={u.enabled} textoActivo="Activo" textoInactivo="Desactivado" />
                  </td>
                  <td className="small">{u.ultimoAcceso ? fechaCortaFecha(u.ultimoAcceso) : 'Nunca'}</td>
                  <td className="small">{u.registro ? fechaCortaFecha(u.registro) : texto(null)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Layout>
  );
}

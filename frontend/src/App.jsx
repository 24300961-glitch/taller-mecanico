import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Registro from './pages/Registro.jsx';
import Recuperar from './pages/Recuperar.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Clientes from './pages/Clientes.jsx';
import Vehiculos from './pages/Vehiculos.jsx';
import Talleres from './pages/Talleres.jsx';
import Historial from './pages/Historial.jsx';
import Usuarios from './pages/Usuarios.jsx';
import Roles from './pages/Roles.jsx';
import Documentacion from './pages/Documentacion.jsx';

function Cargando() {
  return (
    <div className="d-flex align-items-center justify-content-center vh-100">
      <div className="spinner-border text-primary" role="status" aria-label="Cargando" />
    </div>
  );
}

/* Rutas públicas: con sesión redirigen al panel (igual que Thymeleaf). */
function RutaPublica({ children }) {
  const { me, cargando } = useAuth();
  if (cargando) return <Cargando />;
  if (me) return <Navigate to="/dashboard" replace />;
  return children;
}

/* Rutas privadas con guardia de permiso (réplica de @PreAuthorize). */
function RutaPrivada({ permiso, algunPermiso, rolAdmin, algunRol, children }) {
  const { me, cargando, tienePermiso } = useAuth();
  const location = useLocation();
  if (cargando) return <Cargando />;
  if (!me) return <Navigate to="/login?sesion=requerida" replace state={{ from: location.pathname }} />;
  if (permiso && !tienePermiso(permiso)) return <SinPermiso />;
  if (algunPermiso && !algunPermiso.some((p) => tienePermiso(p))) return <SinPermiso />;
  if (rolAdmin && me.rol !== 'Administrador de taller') return <SinPermiso />;
  if (algunRol && !algunRol.includes(me.rol)) return <SinPermiso />;
  return children;
}

function SinPermiso() {
  return (
    <Layout titulo="Sin permiso" subtitulo="Acceso restringido">
      <div className="alert alert-danger" role="alert">
        No tienes permisos suficientes para acceder a esta sección.
      </div>
    </Layout>
  );
}

function NoEncontrada() {
  const { me } = useAuth();
  return (
    <Layout titulo="No encontrada" subtitulo="Recurso inexistente">
      <div className="alert alert-warning" role="alert">
        La página que buscas no existe o fue movida.
      </div>
      <a className="btn btn-accent" href={me ? '/dashboard' : '/login'}>
        {me ? 'Volver al panel' : 'Ir al inicio de sesión'}
      </a>
    </Layout>
  );
}

function Raiz() {
  const { me, cargando } = useAuth();
  if (cargando) return <Cargando />;
  return <Navigate to={me ? '/dashboard' : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Raiz />} />
      <Route
        path="/login"
        element={
          <RutaPublica>
            <Login />
          </RutaPublica>
        }
      />
      <Route
        path="/registro"
        element={
          <RutaPublica>
            <Registro />
          </RutaPublica>
        }
      />
      <Route
        path="/recuperar"
        element={
          <RutaPublica>
            <Recuperar />
          </RutaPublica>
        }
      />
      <Route
        path="/reset-password"
        element={
          <RutaPublica>
            <ResetPassword />
          </RutaPublica>
        }
      />
      <Route
        path="/dashboard"
        element={
          <RutaPrivada>
            <Dashboard />
          </RutaPrivada>
        }
      />
      <Route
        path="/clientes"
        element={
          <RutaPrivada algunRol={['Administrador de taller', 'Recepcionista']}>
            <Clientes />
          </RutaPrivada>
        }
      />
      <Route
        path="/vehiculos"
        element={
          <RutaPrivada permiso="VEHICULOS_CONSULTAR">
            <Vehiculos />
          </RutaPrivada>
        }
      />
      <Route
        path="/historial"
        element={
          <RutaPrivada permiso="VEHICULOS_CONSULTAR">
            <Historial />
          </RutaPrivada>
        }
      />
      <Route
        path="/talleres"
        element={
          <RutaPrivada rolAdmin>
            <Talleres />
          </RutaPrivada>
        }
      />
      <Route
        path="/usuarios"
        element={
          <RutaPrivada permiso="USUARIOS_CONSULTAR">
            <Usuarios />
          </RutaPrivada>
        }
      />
      <Route
        path="/roles"
        element={
          <RutaPrivada algunPermiso={['ROLES_CREAR', 'ROLES_ASIGNAR']}>
            <Roles />
          </RutaPrivada>
        }
      />
      <Route
        path="/documentacion"
        element={
          <RutaPrivada rolAdmin>
            <Documentacion />
          </RutaPrivada>
        }
      />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}

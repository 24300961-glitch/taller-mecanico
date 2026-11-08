import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

const ICONOS = {
  dashboard:
    '<path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 7.5v7a.5.5 0 0 0 .5.5h9a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.146-.354zM2.5 8.707V14.5h3.5V11a.5.5 0 0 1 .5-.5h3a.5.5 0 0 1 .5.5v3.5h3.5V8.707L8 3.207z"/>',
  clientes:
    '<path d="M11 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m8-7a7 7 0 0 0-5.468 11.37C3.242 11.226 4.805 10 8 10s4.757 1.225 5.468 2.37A7 7 0 0 0 8 1"/>',
  talleres:
    '<path d="M2.97 1.35A1 1 0 0 1 3.73 1h8.54a1 1 0 0 1 .76.35l2.609 3.044A1.5 1.5 0 0 1 16 5.37v.255a2.375 2.375 0 0 1-4.25 1.458A2.37 2.37 0 0 1 9.875 8 2.37 2.37 0 0 1 8 7.083 2.37 2.37 0 0 1 6.125 8a2.37 2.37 0 0 1-1.875-.917A2.375 2.375 0 0 1 0 5.625V5.37a1.5 1.5 0 0 1 .361-.976zm1.78 4.275a1.375 1.375 0 0 0 2.75 0 .5.5 0 0 1 1 0 1.375 1.375 0 0 0 2.75 0 .5.5 0 0 1 1 0 1.375 1.375 0 1 0 2.75 0V5.37a.5.5 0 0 0-.12-.325L12.27 2H3.73L1.12 5.045A.5.5 0 0 0 1 5.37v.255a1.375 1.375 0 0 0 2.418.91z"/>',
  vehiculos:
    '<path d="M5 11a1 1 0 1 1 0 2 1 1 0 0 1 0-2m6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2m-4.5-5h3l1.5 3h-6zM1 4.5A1.5 1.5 0 0 1 2.5 3h1.148a1.5 1.5 0 0 1 1.052.447l1.534 1.536A1.5 1.5 0 0 1 6.234 6H13.5A1.5 1.5 0 0 1 15 7.5v3a1.5 1.5 0 0 1-1.5 1.5h-.344a2 2 0 0 0-3.312 0H6.156a2 2 0 0 0-3.312 0H1.5A1.5 1.5 0 0 1 0 10.5v-5A1.5 1.5 0 0 1 1 4.5m1.5 1a.5.5 0 0 0-.5.5v5a.5.5 0 0 0 .5.5h.344a2 2 0 0 1 1.33-1.5L3 8.5a.5.5 0 0 1 .5-.5h8l-.658-1.317A.5.5 0 0 0 10.395 6H6.234l-.8-.8A.5.5 0 0 0 5.08 5H2.5z"/>',
  historial:
    '<path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.006zm2.004.45a7 7 0 0 0-.985-.299l.219-.976q.576.129 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654zm1.038.74a7 7 0 0 0-.658-.623l.64-.674a8 8 0 0 1 .83.798zm2.039 1.196a7 7 0 0 0-.372-.614l.774-.624a8 8 0 0 1 .5.718zm.944.803a7 7 0 0 0-.526-.72l.666-.636a8 8 0 0 1 .861 1.057zM12 10a4 4 0 1 1-8 0 4 4 0 0 1 8 0m4-1a5 5 0 0 0-10 0v3.5a.5.5 0 0 0 1 0V9a4 4 0 0 1 8 0z"/>',
  usuarios:
    '<path d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1zm-7.978-1L7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002-.014.002zM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4m3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0M6.936 9.28a6 6 0 0 0-1.23-.247A7 7 0 0 0 5 9c-4 0-5 3-5 4 0 .667.333 1 1 1h4.216A2.24 2.24 0 0 1 5 13c0-1.01.377-2.042 1.09-2.904.243-.294.526-.569.846-.816M4.92 10A5.5 5.5 0 0 0 4 13H1c0-.26.164-1.03.76-1.72C2.312 10.629 3.282 10 5 10c.935 0 1.855.287 2.464.868A6.5 6.5 0 0 1 4.92 10"/>',
  roles:
    '<path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2m3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2M5 8h6a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1"/>',
};

function Icono({ nombre }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      fill="currentColor"
      viewBox="0 0 16 16"
      aria-hidden="true"
      dangerouslySetInnerHTML={{ __html: ICONOS[nombre] }}
    />
  );
}

function EntradasMenu() {
  const { me, tienePermiso } = useAuth();
  const esAdminTaller = me && me.rol === 'Administrador de taller';
  const entradas = [
    { to: '/dashboard', texto: 'Panel principal', icono: 'dashboard', mostrar: true },
    { to: '/clientes', texto: 'Clientes', icono: 'clientes', mostrar: !!me && ['Administrador de taller', 'Recepcionista'].includes(me.rol) },
    { to: '/talleres', texto: 'Talleres', icono: 'talleres', mostrar: esAdminTaller },
    { to: '/vehiculos', texto: 'Vehículos', icono: 'vehiculos', mostrar: tienePermiso('VEHICULOS_CONSULTAR') },
    { to: '/historial', texto: 'Historial', icono: 'historial', mostrar: tienePermiso('VEHICULOS_CONSULTAR') },
    { to: '/usuarios', texto: 'Usuarios', icono: 'usuarios', mostrar: tienePermiso('USUARIOS_CONSULTAR') },
    {
      to: '/roles',
      texto: 'Asignar roles',
      icono: 'roles',
      mostrar: tienePermiso('ROLES_CREAR') || tienePermiso('ROLES_ASIGNAR'),
    },
  ];
  return (
    <>
      {entradas
        .filter((e) => e.mostrar)
        .map((e) => (
          <NavLink key={e.to} to={e.to} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            <Icono nombre={e.icono} />
            {e.texto}
          </NavLink>
        ))}
    </>
  );
}

export default function Layout({ titulo, subtitulo, children }) {
  const { me, logout } = useAuth();
  const navigate = useNavigate();
  const inicial = me && me.fullName ? me.fullName.trim().charAt(0).toUpperCase() : 'U';

  const salir = async (ev) => {
    ev.preventDefault();
    await logout();
    navigate('/login?logout=true');
  };

  return (
    <div className="container-fluid">
      <div className="row g-0">
        <aside className="col-lg-2 col-md-3 sidebar d-none d-md-block vh-100 overflow-auto">
          <div className="brand d-flex align-items-center">
            <span className="brand-badge">TN</span>
            <div>
              <div className="fw-bold small">Taller Norte</div>
              <small style={{ color: '#94a3b8', fontSize: '0.72rem' }}>Portal interno</small>
            </div>
          </div>
          <nav className="nav flex-column py-2">
            <EntradasMenu />
          </nav>
        </aside>
        <main className="col-lg-10 col-md-9 ms-md-auto">
          <header className="topbar d-flex align-items-center justify-content-between sticky-top">
            <div className="d-flex align-items-center gap-2">
              <button
                className="btn btn-sm btn-outline-secondary d-md-none px-2"
                type="button"
                data-bs-toggle="offcanvas"
                data-bs-target="#sidebarOffcanvas"
                aria-label="Abrir menú"
              >
                &#9776;
              </button>
              <div>
                <h1 className="m-0 fw-semibold page-title h5">{titulo}</h1>
                <small className="text-muted">{subtitulo}</small>
              </div>
            </div>
            <div className="dropdown">
              <button
                className="btn btn-link p-0 m-0 d-flex align-items-center gap-2 text-decoration-none dropdown-toggle"
                data-bs-toggle="dropdown"
                type="button"
                aria-expanded="false"
              >
                <span className="avatar">{inicial}</span>
                <span className="text-start d-none d-sm-block">
                  <span className="d-block fw-semibold text-dark small">{me ? me.fullName : 'Usuario'}</span>
                  <span className="role-badge">{me && me.rol ? me.rol : 'Sin rol'}</span>
                </span>
              </button>
              <ul className="dropdown-menu dropdown-menu-end shadow-sm">
                <li>
                  <h6 className="dropdown-header">{me ? me.email : ''}</h6>
                </li>
                <li>
                  <hr className="dropdown-divider" />
                </li>
                <li>
                  <a href="/logout" onClick={salir} className="dropdown-item text-danger fw-semibold">
                    Cerrar sesión
                  </a>
                </li>
              </ul>
            </div>
          </header>
          <section className="p-3 p-sm-4 p-lg-5">{children}</section>
        </main>
      </div>
      <div
        className="offcanvas offcanvas-start offcanvas-tn"
        tabIndex="-1"
        id="sidebarOffcanvas"
        aria-label="Menú"
      >
        <div className="offcanvas-header justify-content-between">
          <div className="d-flex align-items-center">
            <span className="brand-badge">TN</span>
            <div className="fw-bold text-white">Taller Norte</div>
          </div>
          <button type="button" className="btn-close btn-close-white" data-bs-dismiss="offcanvas" aria-label="Cerrar menú" />
        </div>
        <div className="offcanvas-body p-0">
          <nav className="nav flex-column py-2" data-bs-dismiss="offcanvas">
            <EntradasMenu />
          </nav>
        </div>
      </div>
    </div>
  );
}

export function VolverLogin() {
  return (
    <Link to="/login" className="link-muted d-inline-flex align-items-center mb-3 small">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        fill="currentColor"
        className="me-2"
        viewBox="0 0 16 16"
      >
        <path
          fillRule="evenodd"
          d="M15 8a.5.5 0 0 0-.5-.5H2.707l3.147-3.146a.5.5 0 1 0-.708-.708l-4 4a.5.5 0 0 0 0 .708l4 4a.5.5 0 0 0 .708-.708L2.707 8.5H14.5A.5.5 0 0 0 15 8z"
        />
      </svg>
      Volver al inicio de sesión
    </Link>
  );
}

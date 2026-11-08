import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import BrandPanel from '../components/BrandPanel.jsx';
import { Alerta, BotonCargando, PasswordConOjo, useAlerta } from '../components/ui.jsx';

const AVISOS = {
  error: 'Correo o contraseña incorrectos. Verifica tus datos e inténtalo de nuevo.',
  logout: 'Sesión cerrada correctamente.',
  reset: 'Contraseña restablecida. Inicia sesión con tu nueva contraseña.',
  registro: 'Cuenta creada correctamente. Inicia sesión con tu correo y contraseña.',
  password: 'Contraseña actualizada. Por seguridad, inicia sesión nuevamente.',
  expirada: 'Tu sesión se cerró porque cambiaron tus permisos o tu contraseña. Inicia sesión de nuevo.',
  sesion: 'Inicia sesión para continuar.',
};

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errores, setErrores] = useState({});
  const [ocupado, setOcupado] = useState(false);
  const { alerta, mostrar, mostrarError, limpiar } = useAlerta();

  const avisoInicial = (() => {
    if (params.get('error') === 'desactivada')
      return { tipo: 'danger', mensaje: 'Tu cuenta está desactivada. Contacta al administrador del taller.' };
    for (const k of Object.keys(AVISOS)) {
      if (params.get(k) !== null)
        return { tipo: k === 'error' ? 'danger' : k === 'expirada' || k === 'sesion' ? 'warning' : 'success', mensaje: AVISOS[k] };
    }
    return null;
  })();

  const enviar = async (ev) => {
    ev.preventDefault();
    limpiar();
    const e = {};
    if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) e.email = 'Ingresa un correo electrónico válido';
    if (!password) e.password = 'Ingresa tu contraseña';
    setErrores(e);
    if (Object.keys(e).length > 0) {
      mostrar('danger', 'Completa correctamente tu correo y contraseña.');
      return;
    }
    setOcupado(true);
    try {
      const r = await login(email.trim(), password);
      if (r.ok) navigate('/dashboard', { replace: true });
      else mostrar('danger', r.mensaje);
    } catch (err) {
      mostrarError(err);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <div className="container-fluid login-shell">
      <div className="row g-0 login-shell">
        <BrandPanel variante="login" />
        <div className="col-lg-6 form-panel d-flex align-items-center justify-content-center p-3 p-sm-4 p-md-5">
          <div className="w-100 py-4" style={{ maxWidth: '440px' }}>
            <div className="card card-login p-3 p-sm-4 p-md-5">
              <h2 className="h4 fw-bold mb-1">Bienvenido de nuevo</h2>
              <p className="text-muted mb-4">Ingresa a tu cuenta para continuar.</p>
              {avisoInicial && <Alerta tipo={avisoInicial.tipo} mensaje={avisoInicial.mensaje} onCerrar={() => {}} />}
              {alerta && <Alerta tipo={alerta.tipo} mensaje={alerta.mensaje} detalles={alerta.detalles} onCerrar={limpiar} />}
              <form noValidate onSubmit={enviar}>
                <div className="mb-3">
                  <label htmlFor="email" className="form-label">Correo electrónico</label>
                  <input
                    type="email" id="email" name="email" className={`form-control${errores.email ? ' is-invalid' : ''}`}
                    maxLength="190" placeholder="tu@correo.com" required autoFocus autoComplete="email"
                    value={email} onChange={(e) => setEmail(e.target.value)}
                  />
                  {errores.email && <div className="invalid-feedback">{errores.email}</div>}
                </div>
                <div className="mb-3">
                  <label htmlFor="password" className="form-label">Contraseña</label>
                  <PasswordConOjo
                    id="password" name="password" placeholder="••••••••" required autoComplete="current-password"
                    maxLength="72" value={password} onChange={(e) => setPassword(e.target.value)}
                    className={`form-control${errores.password ? ' is-invalid' : ''}`}
                  />
                  {errores.password && <div className="invalid-feedback d-block">{errores.password}</div>}
                </div>
                <div className="d-flex justify-content-end mb-4">
                  <Link to="/recuperar" className="small link-muted">¿Olvidaste tu contraseña?</Link>
                </div>
                <BotonCargando type="submit" className="btn btn-login-primary w-100" cargando={ocupado} textoCargando="Verificando...">
                  Entrar al portal
                </BotonCargando>
              </form>
              <div className="my-4 text-center divider-text">— o —</div>
              <div className="text-center">
                <p className="mb-2 text-muted small">¿No tienes cuenta?</p>
                <Link to="/registro" className="btn btn-outline-primary w-100" style={{ minHeight: '50px', borderRadius: '12px', fontWeight: 600 }}>
                  Crear cuenta nueva
                </Link>
              </div>
            </div>
            <div className="text-center mt-4 px-2">
              <small className="text-muted">Al continuar aceptas nuestras políticas de privacidad y términos de uso.</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

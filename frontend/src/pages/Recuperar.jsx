import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import BrandPanel from '../components/BrandPanel.jsx';
import { Alerta, BotonCargando, Campo, useAlerta } from '../components/ui.jsx';
import { VolverLogin } from '../components/Layout.jsx';

export default function Recuperar() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const { alerta, mostrar, mostrarError, limpiar } = useAlerta();
  const [enlace, setEnlace] = useState(null);

  const enviar = async (ev) => {
    ev.preventDefault();
    limpiar();
    setEnlace(null);
    const v = email.trim();
    if (!v || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v) || v.length > 190) {
      setError('Ingresa un correo electrónico válido');
      mostrar('danger', 'Ingresa un correo electrónico válido.');
      return;
    }
    setError('');
    setOcupado(true);
    try {
      const r = await api('/api/auth/forgot-password', { method: 'POST', body: { email: v, channel: 'EMAIL' } });
      if (r.enlace) {
        mostrar('warning', r.message);
        setEnlace(r.enlace);
      } else {
        mostrar('success', r.message);
      }
    } catch (e) {
      mostrarError(e);
      if (e.data && e.data.errores && e.data.errores.email) setError(e.data.errores.email);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <div className="container-fluid login-shell">
      <div className="row g-0 login-shell">
        <BrandPanel variante="recuperar" />
        <div className="col-lg-6 form-panel d-flex align-items-center justify-content-center p-3 p-sm-4 p-md-5">
          <div className="w-100 py-4" style={{ maxWidth: '440px' }}>
            <VolverLogin />
            <div className="card card-form p-3 p-sm-4 p-md-5">
              <h2 className="h4 fw-bold mb-1">Restablece tu contraseña</h2>
              <p className="text-muted mb-4">Te ayudaremos a generar una nueva en pocos pasos.</p>
              <div className="info-box mb-4">
                Ingresa el correo electrónico asociado a tu cuenta para crear una nueva contraseña.
              </div>
              {alerta && (
                <Alerta
                  tipo={alerta.tipo}
                  mensaje={alerta.mensaje}
                  detalles={alerta.detalles}
                  onCerrar={() => {
                    limpiar();
                    setEnlace(null);
                  }}
                />
              )}
              {enlace && (
                <div className="alert alert-warning" role="status">
                  <a className="fw-bold d-block mt-1" href={enlace}>
                    Abrir enlace de restablecimiento
                  </a>
                </div>
              )}
              <form noValidate onSubmit={enviar}>
                <Campo label="Correo electrónico" required htmlFor="email" error={error} className="mb-4">
                  <input
                    type="email" id="email" name="email" className={`form-control${error ? ' is-invalid' : ''}`}
                    maxLength="190" placeholder="tu@correo.com" required autoComplete="email"
                    value={email} onChange={(e) => setEmail(e.target.value)}
                  />
                </Campo>
                <BotonCargando type="submit" className="btn btn-login-primary w-100 mb-3" cargando={ocupado} textoCargando="Enviando...">
                  Enviar instrucciones
                </BotonCargando>
              </form>
              <div className="text-center pt-3 border-top">
                <p className="mb-0 small text-muted">
                  ¿Ya la recordaste? <Link to="/login" className="fw-semibold link-muted">Inicia sesión</Link>
                </p>
              </div>
            </div>
            <div className="text-center mt-4 px-2">
              <small className="text-muted">Taller Norte · Soporte y seguridad</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

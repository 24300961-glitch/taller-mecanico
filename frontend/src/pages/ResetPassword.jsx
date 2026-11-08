import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, requisitosContrasena } from '../api.js';
import BrandPanel from '../components/BrandPanel.jsx';
import { Alerta, BotonCargando, Campo, PasswordConOjo, useAlerta } from '../components/ui.jsx';
import { VolverLogin } from '../components/Layout.jsx';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [estado, setEstado] = useState('verificando'); // verificando | valido | invalido
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [errores, setErrores] = useState({});
  const [ocupado, setOcupado] = useState(false);
  const [bloqueado, setBloqueado] = useState(false);
  const { alerta, mostrar, mostrarError, limpiar } = useAlerta();

  useEffect(() => {
    api(`/api/auth/validar-token?token=${encodeURIComponent(token)}`)
      .then((r) => setEstado(r.valido ? 'valido' : 'invalido'))
      .catch(() => setEstado('invalido'));
  }, [token]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const enviar = async (ev) => {
    ev.preventDefault();
    limpiar();
    const e = {};
    const f = requisitosContrasena(form.newPassword);
    if (form.newPassword && f.length) e.newPassword = 'La contraseña no cumple: ' + f.join(', ');
    else if (!form.newPassword) e.newPassword = 'La nueva contraseña es obligatoria';
    if (!form.confirmPassword) e.confirmPassword = 'Confirma la nueva contraseña';
    else if (form.confirmPassword !== form.newPassword) e.confirmPassword = 'Las contraseñas no coinciden';
    setErrores(e);
    if (Object.keys(e).length > 0) {
      mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    setOcupado(true);
    try {
      const r = await api('/api/auth/reset-password', {
        method: 'POST',
        body: { token, newPassword: form.newPassword, confirmPassword: form.confirmPassword },
      });
      mostrar('success', (r.message || 'Contraseña actualizada.') + ' Redirigiendo al inicio de sesión...');
      setBloqueado(true);
      setTimeout(() => {
        window.location.href = '/login?reset=true';
      }, 1800);
    } catch (err) {
      setOcupado(false);
      mostrarError(err);
      if (err.data && err.data.errores) {
        const porCampo = {};
        Object.keys(err.data.errores).forEach((k) => {
          porCampo[k] = err.data.errores[k];
        });
        setErrores(porCampo);
      }
    }
  };

  return (
    <div className="container-fluid login-shell">
      <div className="row g-0 login-shell">
        <BrandPanel variante="reset" />
        <div className="col-lg-6 form-panel d-flex align-items-center justify-content-center p-3 p-sm-4 p-md-5">
          <div className="w-100 py-4" style={{ maxWidth: '440px' }}>
            <VolverLogin />
            {estado === 'verificando' && (
              <div className="card card-form p-5 text-center">
                <div className="spinner-border text-primary mx-auto" role="status" aria-label="Verificando enlace" />
                <p className="text-muted mt-3 mb-0">Verificando el enlace...</p>
              </div>
            )}
            {estado === 'valido' && (
              <div className="card card-form p-3 p-sm-4 p-md-5">
                <h2 className="h4 fw-bold mb-1">Restablecimiento confirmado</h2>
                <p className="text-muted mb-4">Tu enlace es válido. Ingresa tu nueva contraseña a continuación.</p>
                <div className="info-box mb-4">
                  La contraseña debe tener de 12 a 72 caracteres e incluir mayúscula, minúscula, número y símbolo.
                </div>
                {alerta && <Alerta tipo={alerta.tipo} mensaje={alerta.mensaje} detalles={alerta.detalles} onCerrar={limpiar} />}
                <fieldset disabled={bloqueado} style={{ border: 0, padding: 0, margin: 0 }}>
                  <form noValidate onSubmit={enviar}>
                    <Campo label="Nueva contraseña" required htmlFor="newPassword" error={errores.newPassword}>
                      <PasswordConOjo
                        id="newPassword" name="newPassword" placeholder="Mínimo 12 caracteres" minLength="12"
                        maxLength="72" required autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')}
                        className={`form-control${errores.newPassword ? ' is-invalid' : ''}`}
                      />
                    </Campo>
                    <Campo label="Confirmar nueva contraseña" required htmlFor="newPassword2" error={errores.confirmPassword} className="mb-4">
                      <PasswordConOjo
                        id="newPassword2" name="confirmPassword" placeholder="Repite la nueva contraseña" maxLength="72"
                        required autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')}
                        className={`form-control${errores.confirmPassword ? ' is-invalid' : ''}`}
                      />
                    </Campo>
                    <BotonCargando type="submit" className="btn btn-login-primary w-100 mb-3" cargando={ocupado} textoCargando="Actualizando...">
                      Actualizar contraseña
                    </BotonCargando>
                  </form>
                </fieldset>
                <div className="text-center pt-3 border-top">
                  <p className="mb-0 small text-muted">Una vez actualizada, serás redirigido al inicio de sesión.</p>
                </div>
              </div>
            )}
            {estado === 'invalido' && (
              <div className="card card-form p-3 p-sm-4 p-md-5">
                <div className="text-center py-3">
                  <div
                    className="mx-auto mb-4 d-inline-flex align-items-center justify-content-center fw-bold"
                    style={{ background: '#dc2626', color: '#fff', width: '52px', height: '52px', borderRadius: '12px', fontSize: '1.4rem' }}
                  >
                    !
                  </div>
                  <h2 className="h4 fw-bold mb-1" style={{ color: '#991b1b' }}>Enlace inválido o expirado</h2>
                  <p className="text-muted mb-4">
                    Este enlace de recuperación ya fue utilizado, caducó o no es válido. Por favor solicita uno nuevo.
                  </p>
                  <Link to="/recuperar" className="btn btn-login-primary w-100">
                    Solicitar nuevo enlace
                  </Link>
                </div>
              </div>
            )}
            <div className="text-center mt-4 px-2">
              <small className="text-muted">Taller Norte · Seguridad de cuentas</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

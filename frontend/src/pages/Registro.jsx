import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, requisitosContrasena, RX } from '../api.js';
import BrandPanel from '../components/BrandPanel.jsx';
import { Alerta, BotonCargando, Campo, PasswordConOjo, useAlerta, useToasts } from '../components/ui.jsx';
import { VolverLogin } from '../components/Layout.jsx';

export default function Registro() {
  const [rolRegistro, setRolRegistro] = useState('predeterminado');
  const [form, setForm] = useState({ fullName: '', email: '', telefono: '', password: '', confirmPassword: '' });
  const [errores, setErrores] = useState({});
  const [faltan, setFaltan] = useState([]);
  const [ocupado, setOcupado] = useState(false);
  const [bloqueado, setBloqueado] = useState(false);
  const { alerta, mostrar, mostrarError, limpiar } = useAlerta();
  const { push, zona } = useToasts();

  useEffect(() => {
    api('/api/auth/registro-info')
      .then((r) => setRolRegistro(r.rolRegistro || 'predeterminado'))
      .catch(() => {});
  }, []);

  const set = (k) => (e) => {
    const v = e.target.value;
    setForm((f) => ({ ...f, [k]: v }));
    if (k === 'password') setFaltan(requisitosContrasena(v));
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    limpiar();
    const d = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      telefono: form.telefono.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
    };
    const e = {};
    if (!d.fullName || !RX.NOMBRE_100.test(d.fullName))
      e.fullName = 'El nombre debe tener de 2 a 100 caracteres y solo letras, espacios, puntos, apóstrofos o guiones';
    if (!d.email || d.email.length > 190 || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.email))
      e.email = 'Ingresa un correo válido';
    if (d.telefono && !/^\+[1-9]\d{7,14}$/.test(d.telefono))
      e.telefono = 'Usa formato internacional, por ejemplo +525512345678';
    const f = requisitosContrasena(d.password);
    if (d.password && f.length) e.password = 'La contraseña no cumple: ' + f.join(', ');
    else if (!d.password) e.password = 'La contraseña es obligatoria';
    if (!d.confirmPassword) e.confirmPassword = 'Confirma tu contraseña';
    else if (d.confirmPassword !== d.password) e.confirmPassword = 'Las contraseñas no coinciden';
    setErrores(e);
    if (Object.keys(e).length > 0) {
      mostrar('danger', 'Corrige los campos marcados antes de continuar.');
      return;
    }
    setOcupado(true);
    try {
      const body = {
        email: d.email,
        fullName: d.fullName,
        password: d.password,
        confirmPassword: d.confirmPassword,
        recoveryPhone: d.telefono || null,
      };
      const r = await api('/api/auth/register', { method: 'POST', body });
      mostrar('success', (r.message || 'Cuenta creada correctamente.') + ' Redirigiendo al inicio de sesión...');
      push(r.message || 'Cuenta creada correctamente.', true);
      setBloqueado(true);
      setTimeout(() => {
        window.location.href = (r.redirect || '/login?registro=ok').startsWith('/') ? r.redirect || '/login?registro=ok' : '/login?registro=ok';
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
      } else if (err.status === 409) {
        setErrores({ email: err.message });
      }
    }
  };

  return (
    <div className="container-fluid login-shell">
      <div className="row g-0 login-shell">
        <BrandPanel variante="registro" />
        <div className="col-lg-6 form-panel d-flex align-items-center justify-content-center p-3 p-sm-4 p-md-5">
          <div className="w-100 py-4" style={{ maxWidth: '460px' }}>
            <VolverLogin />
            <div className="card card-form p-3 p-sm-4 p-md-5">
              <h2 className="h4 fw-bold mb-1">Registro de usuario</h2>
              <p className="text-muted mb-4">Completa tus datos para crear tu perfil.</p>
              <div className="role-hint mb-4">
                Las cuentas nuevas se crean con el rol <b>{rolRegistro}</b>. Si necesitas otro rol, comunícate con el
                administrador del taller. Después de registrarte deberás iniciar sesión con tu correo y contraseña.
              </div>
              {alerta && <Alerta tipo={alerta.tipo} mensaje={alerta.mensaje} detalles={alerta.detalles} onCerrar={limpiar} />}
              <fieldset disabled={bloqueado} style={{ border: 0, padding: 0, margin: 0 }}>
                <form noValidate onSubmit={enviar}>
                  <Campo label="Nombre completo" required htmlFor="fullName" error={errores.fullName}>
                    <input
                      type="text" id="fullName" name="fullName" className={`form-control${errores.fullName ? ' is-invalid' : ''}`}
                      placeholder="Ej. Juan Pérez García" required minLength="2" maxLength="100" autoComplete="name"
                      value={form.fullName} onChange={set('fullName')}
                    />
                  </Campo>
                  <Campo label="Correo electrónico" required htmlFor="email" error={errores.email}>
                    <input
                      type="email" id="email" name="email" className={`form-control${errores.email ? ' is-invalid' : ''}`}
                      placeholder="tu@correo.com" required maxLength="190" autoComplete="email"
                      value={form.email} onChange={set('email')}
                    />
                  </Campo>
                  <Campo label="Teléfono (opcional)" htmlFor="telefono" error={errores.telefono}>
                    <input
                      type="tel" id="telefono" name="telefono" className={`form-control${errores.telefono ? ' is-invalid' : ''}`}
                      placeholder="+525512345678" autoComplete="tel" value={form.telefono} onChange={set('telefono')}
                    />
                  </Campo>
                  <Campo label="Contraseña" required htmlFor="password" error={errores.password}>
                    <PasswordConOjo
                      id="password" name="password" placeholder="Mínimo 12 caracteres" minLength="12" maxLength="72"
                      required autoComplete="new-password" value={form.password} onChange={set('password')}
                      className={`form-control${errores.password ? ' is-invalid' : ''}`}
                    />
                    <ul className="small mt-2 mb-0 text-muted">
                      {(faltan.length ? faltan.map((f) => `Falta: ${f}`) : ['La contraseña cumple con los requisitos']).map(
                        (t, i) => (
                          <li key={i} className={faltan.length ? '' : 'text-success'}>
                            {t}
                          </li>
                        )
                      )}
                    </ul>
                    {!faltan.length && form.password === '' && (
                      <ul className="small mt-2 mb-0 text-muted">
                        <li>12 a 72 caracteres con mayúscula, minúscula, número y símbolo.</li>
                      </ul>
                    )}
                  </Campo>
                  <Campo label="Confirmar contraseña" required htmlFor="password2" error={errores.confirmPassword} className="mb-4">
                    <PasswordConOjo
                      id="password2" name="confirmPassword" placeholder="Repite tu contraseña" maxLength="72"
                      required autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')}
                      className={`form-control${errores.confirmPassword ? ' is-invalid' : ''}`}
                    />
                  </Campo>
                  <BotonCargando type="submit" className="btn btn-login-primary w-100 mb-3" cargando={ocupado} textoCargando="Creando cuenta...">
                    Crear mi cuenta
                  </BotonCargando>
                </form>
              </fieldset>
              <div className="text-center pt-3 border-top">
                <p className="mb-0 text-muted small">
                  ¿Ya tienes cuenta? <Link to="/login" className="fw-semibold link-muted">Inicia sesión aquí</Link>
                </p>
              </div>
            </div>
            <div className="text-center mt-4 px-2">
              <small className="text-muted">Taller Norte · Portal de clientes y equipo técnico</small>
            </div>
          </div>
        </div>
      </div>
      {zona}
    </div>
  );
}

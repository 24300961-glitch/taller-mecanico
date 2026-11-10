import { useEffect, useState } from 'react';
import { texto } from '../api.js';

/* Alerta tipo Bootstrap con cierre. `detalles` es lista de viñetas. */
export function Alerta({ tipo, mensaje, detalles, onCerrar }) {
  if (!mensaje) return null;
  return (
    <div className={`alert alert-${tipo} alert-dismissible fade show mb-3`} role={tipo === 'danger' ? 'alert' : 'status'}>
      <div>{mensaje}</div>
      {detalles && detalles.length > 0 && (
        <ul className="mb-0 mt-2 small">
          {detalles.map((d, i) => (
            <li key={i}>{d}</li>
          ))}
        </ul>
      )}
      <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar} />
    </div>
  );
}

/* Estado de una llamada que puede fallar: guarda {mensaje, detalles}. */
export function useAlerta() {
  const [alerta, setAlerta] = useState(null);
  const limpiar = () => setAlerta(null);
  const mostrar = (tipo, mensaje, detalles) => setAlerta({ tipo, mensaje, detalles });
  const mostrarError = (e) => {
    const detalles = [];
    if (e && e.data && e.data.errores) {
      Object.keys(e.data.errores).forEach((k) => detalles.push(e.data.errores[k]));
    }
    mostrar(e && e.status === 409 ? 'warning' : 'danger', (e && e.message) || 'Ocurrió un error inesperado.', detalles);
  };
  return { alerta, limpiar, mostrar, mostrarError, setAlerta };
}

export function Insignia({ texto: t, clase }) {
  return <span className={`badge rounded-pill ${clase}`}>{t}</span>;
}

export function EstadoBadge({ activo, textoActivo = 'Activo', textoInactivo = 'Desactivado' }) {
  return activo ? (
    <Insignia texto={textoActivo} clase="text-bg-success" />
  ) : (
    <Insignia texto={textoInactivo} clase="text-bg-secondary" />
  );
}

export function BotonCargando({ cargando, textoCargando, children, ...props }) {
  return (
    <button disabled={cargando} {...props}>
      {cargando && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
      {cargando ? textoCargando || 'Procesando...' : children}
    </button>
  );
}

/* Campo de formulario con mensaje de error. */
export function Campo({ label, error, children, required, htmlFor, className = 'mb-3' }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={htmlFor} className="form-label">
          {label}
          {required ? ' *' : ''}
        </label>
      )}
      {children}
      {error && <div className="invalid-feedback d-block">{error}</div>}
    </div>
  );
}

/* Modal controlado por React con clases de Bootstrap. */
export function Modal({ abierto, titulo, onCerrar, onSubmit, pie, children, grande }) {
  useEffect(() => {
    if (abierto) document.body.classList.add('modal-open');
    else document.body.classList.remove('modal-open');
    return () => document.body.classList.remove('modal-open');
  }, [abierto ]);
  if (!abierto) return null;
  return (
    <>
      <div className="modal fade show d-block" tabIndex="-1" role="dialog" aria-modal="true">
        <div className={`modal-dialog modal-dialog-scrollable${grande ? ' modal-lg' : ''}`}>
          <div className="modal-content">
            <form
              noValidate
              onSubmit={(ev) => {
                ev.preventDefault();
                if (onSubmit) onSubmit(ev);
              }}
            >
              <div className="modal-header">
                <h2 className="modal-title h5">{titulo}</h2>
                <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar} />
              </div>
              <div className="modal-body">{children}</div>
              <div className="modal-footer">
                <button type="button" className="btn btn-outline-secondary" onClick={onCerrar}>
                  Cancelar
                </button>
                {pie}
              </div>
            </form>
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show" onClick={onCerrar} />
    </>
  );
}

/* Lleva la vista al primer campo marcado con error (modales largos). */
export function enfocarPrimerError() {
  setTimeout(() => {
    const campo =
      document.querySelector('.modal.show .is-invalid') || document.querySelector('.is-invalid');
    if (campo) {
      campo.scrollIntoView({ behavior: 'smooth', block: 'center' });
      if (campo.focus) {
        try {
          campo.focus({ preventScroll: true });
        } catch (e) {
          campo.focus();
        }
      }
    }
  }, 60);
}

/* Botón ojo para ver/ocultar contraseñas (decisión 23). */
const OJO =
  '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M8 12.5A4.5 4.5 0 1 1 8 3a4.5 4.5 0 0 1 0 9M8 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4"/>';
const OJO_CERRADO =
  '<path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755q-.247.248-.517.486zM11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829M3.35 5.47q-.27.24-.518.487A13 13 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7 7 0 0 1 8 13.5C2.96 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12z"/>';

export function PasswordConOjo(props) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="input-group">
      <input {...props} type={visible ? 'text' : 'password'} />
      <button
        type="button"
        className="btn btn-outline-secondary"
        aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        onClick={() => setVisible((v) => !v)}
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="18"
          height="18"
          fill="currentColor"
          viewBox="0 0 16 16"
          aria-hidden="true"
          dangerouslySetInnerHTML={{ __html: visible ? OJO_CERRADO : OJO }}
        />
      </button>
    </div>
  );
}

/* Toasts simples sin dependencia del JS de Bootstrap. */
let toastId = 0;

export function useToasts() {
  const [toasts, setToasts] = useState([]);
  const push = (mensaje, ok) => {
    const id = ++toastId;
    setToasts((lista) => [...lista, { id, mensaje, ok }]);
    setTimeout(() => {
      setToasts((lista) => lista.filter((t) => t.id !== id));
    }, ok ? 3500 : 6000);
  };
  const zona = (
    <div className="toast-container position-fixed top-0 end-0 p-3" style={{ zIndex: 1090 }}>
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`toast show align-items-center text-white border-0 ${t.ok ? 'bg-success' : 'bg-danger'}`}
          role={t.ok ? 'status' : 'alert'}
        >
          <div className="d-flex">
            <div className="toast-body small fw-semibold">{t.mensaje}</div>
            <button
              type="button"
              className="btn-close btn-close-white me-2 m-auto"
              aria-label="Cerrar"
              onClick={() => setToasts((lista) => lista.filter((x) => x.id !== t.id))}
            />
          </div>
        </div>
      ))}
    </div>
  );
  return { push, zona };
}

export function celdaTexto(valor) {
  return texto(valor);
}

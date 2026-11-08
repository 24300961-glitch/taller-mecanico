/* Panel de marca de la portada dividida (decisiones 26-27). */

const ICONO_AUTO =
  '<path d="M5 11a1 1 0 1 1 0 2 1 1 0 0 1 0-2m6 0a1 1 0 1 1 0 2 1 1 0 0 1 0-2m-4.5-5h3l1.5 3h-6zM1 4.5A1.5 1.5 0 0 1 2.5 3h1.148a1.5 1.5 0 0 1 1.052.447l1.534 1.536A1.5 1.5 0 0 1 6.234 6H13.5A1.5 1.5 0 0 1 15 7.5v3a1.5 1.5 0 0 1-1.5 1.5h-.344a2 2 0 0 0-3.312 0H6.156a2 2 0 0 0-3.312 0H1.5A1.5 1.5 0 0 1 0 10.5v-5A1.5 1.5 0 0 1 1 4.5m1.5 1a.5.5 0 0 0-.5.5v5a.5.5 0 0 0 .5.5h.344a2 2 0 0 1 1.33-1.5L3 8.5a.5.5 0 0 1 .5-.5h8l-.658-1.317A.5.5 0 0 0 10.395 6H6.234l-.8-.8A.5.5 0 0 0 5.08 5H2.5z"/>';
const ICONO_CANDADO =
  '<path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2m3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2M5 8h6a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1"/>';
const ICONO_PERSONA =
  '<path d="M11 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m8-7a7 7 0 0 0-5.468 11.37C3.242 11.226 4.805 10 8 10s4.757 1.225 5.468 2.37A7 7 0 0 0 8 1"/>';
const ICONO_RELOJ =
  '<path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.006zm2.004.45a7 7 0 0 0-.985-.299l.219-.976q.576.129 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654zm1.038.74a7 7 0 0 0-.658-.623l.64-.674a8 8 0 0 1 .83.798zm2.039 1.196a7 7 0 0 0-.372-.614l.774-.624a8 8 0 0 1 .5.718zm.944.803a7 7 0 0 0-.526-.72l.666-.636a8 8 0 0 1 .861 1.057zM12 10a4 4 0 1 1-8 0 4 4 0 0 1 8 0m4-1a5 5 0 0 0-10 0v3.5a.5.5 0 0 0 1 0V9a4 4 0 0 1 8 0z"/>';
const ICONO_CHECK =
  '<path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16"/><path d="M10.97 4.97a.235.235 0 0 0-.02.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-1.071-1.05"/>';

export const SVG = { ICONO_AUTO, ICONO_CANDADO, ICONO_PERSONA, ICONO_RELOJ, ICONO_CHECK };

function Icono({ codigo }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      fill="currentColor"
      viewBox="0 0 16 16"
      aria-hidden="true"
      dangerouslySetInnerHTML={{ __html: codigo }}
    />
  );
}

const CONTENIDOS = {
  login: {
    etiqueta: 'Portal interno del taller',
    titulo: 'Taller Norte',
    descripcion: 'Servicio automotriz profesional: clientes, vehículos, talleres e historial en un solo lugar.',
    puntos: [
      { icono: ICONO_PERSONA, titulo: 'Clientes y vehículos', texto: 'Expedientes completos con propietario y taller asignado.' },
      { icono: ICONO_RELOJ, titulo: 'Historial por cliente', texto: 'En qué talleres ha dejado sus vehículos y cuándo.' },
      { icono: ICONO_CANDADO, titulo: 'Acceso por roles', texto: 'Cada persona ve solo los módulos de su puesto.' },
    ],
    remate: 'Tu taller, organizado.',
    remateIcono: ICONO_AUTO,
  },
  registro: {
    etiqueta: 'Únete al portal',
    titulo: 'Crea tu cuenta',
    descripcion: 'Regístrate gratis y accede a los servicios del Taller Norte con tu correo.',
    puntos: [
      { icono: ICONO_CANDADO, titulo: 'Cuenta en un minuto', texto: 'Solo tu nombre, correo y una contraseña segura.' },
      { icono: ICONO_AUTO, titulo: 'Tus vehículos a la vista', texto: 'Consulta en qué taller está cada uno.' },
      { icono: ICONO_CANDADO, titulo: 'Protegido desde el inicio', texto: 'Contraseña segura y recuperación por correo.' },
    ],
    remate: 'Tu taller, organizado.',
    remateIcono: ICONO_AUTO,
  },
  recuperar: {
    etiqueta: 'Recupera tu acceso',
    titulo: '¿Olvidaste tu contraseña?',
    descripcion: 'Te ayudamos a generar una nueva en pocos pasos, de forma segura.',
    pasos: ['Escribe tu correo|El mismo con el que creaste tu cuenta.', 'Abre el enlace|Te mostramos un enlace temporal de un solo uso.', 'Crea tu nueva clave|Elige una contraseña segura y vuelve a entrar.'],
    remate: 'Tus datos, protegidos.',
    remateIcono: ICONO_CANDADO,
  },
  reset: {
    etiqueta: 'Crea tu nueva clave',
    titulo: 'Crear nueva contraseña',
    descripcion: 'Elige una contraseña segura que solo tú conozcas.',
    puntos: [
      { icono: ICONO_CANDADO, titulo: '12 a 72 caracteres', texto: 'Con mayúscula, minúscula, número y símbolo.' },
      { icono: ICONO_CHECK, titulo: 'Diferente a la anterior', texto: 'No reutilices contraseñas de otras cuentas.' },
      { icono: ICONO_RELOJ, titulo: 'El enlace es de un solo uso', texto: 'Al guardarla, el enlace queda desactivado.' },
    ],
    remate: 'Tus datos, protegidos.',
    remateIcono: ICONO_CANDADO,
  },
};

export default function BrandPanel({ variante }) {
  const c = CONTENIDOS[variante] || CONTENIDOS.login;
  return (
    <div className="col-lg-6 brand-panel d-flex align-items-center p-4 p-md-5">
      <div className="brand-inner w-100 mx-auto py-4 py-lg-0" style={{ maxWidth: '480px' }}>
        <div className="brand-badge-lg mb-4">TN</div>
        <p className="mini-badge mini-badge-oscura mb-3">{c.etiqueta}</p>
        <h1 className="h3 fw-bold mb-2 brand-title brand-h">{c.titulo}</h1>
        <p className="brand-sub mb-4 mb-lg-5">{c.descripcion}</p>
        {c.puntos && (
          <ul className="feature-list mb-4 mb-lg-5">
            {c.puntos.map((p, i) => (
              <li key={i}>
                <span className="feature-icon">
                  <Icono codigo={p.icono} />
                </span>
                <div>
                  <strong>{p.titulo}</strong>
                  <span>{p.texto}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
        {c.pasos && (
          <ol className="feature-list mb-4 mb-lg-5" style={{ listStyle: 'none' }}>
            {c.pasos.map((p, i) => {
              const [titulo, texto] = p.split('|');
              return (
                <li key={i}>
                  <span className="step-num">{i + 1}</span>
                  <div>
                    <strong>{titulo}</strong>
                    <span>{texto}</span>
                  </div>
                </li>
              );
            })}
          </ol>
        )}
        <p className="brand-auto mb-0">
          <svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true"
            dangerouslySetInnerHTML={{ __html: c.remateIcono }}
          />
          {c.remate}
        </p>
      </div>
    </div>
  );
}

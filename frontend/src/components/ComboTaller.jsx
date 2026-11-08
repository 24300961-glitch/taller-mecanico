import { useEffect, useRef, useState } from 'react';

/** Selector único de taller con búsqueda, lista desplazable y cierre por Escape o clic exterior.
 * @param {{id: number|string, nombre: string}[]} opciones talleres disponibles
 * @param {number|string} valor identificador seleccionado
 * @param {(id: string) => void} onChange callback para guardar el identificador
 * @param {string} etiqueta nombre visible del control
 * @param {string} error mensaje de validación opcional
 * @returns {JSX.Element} control de selección accesible
 * @throws {never} no propaga errores de interfaz
 */
export default function ComboTaller({ opciones, valor, onChange, etiqueta, error }) {
  const [abierto, setAbierto] = useState(false);
  const [filtro, setFiltro] = useState('');
  const contenedor = useRef(null);
  const seleccionado = valor ? opciones.find((t) => String(t.id) === String(valor)) : null;
  const visibles = opciones.filter((t) => String(t.id) !== '' && (t.nombre || '').toLocaleLowerCase().includes(filtro.trim().toLocaleLowerCase()));

  useEffect(() => {
    if (!abierto) return undefined;
    const cerrarFuera = (evento) => {
      // Cierra el menú si el clic ocurre fuera de este control.
      if (contenedor.current && !contenedor.current.contains(evento.target)) setAbierto(false);
    };
    const cerrarEscape = (evento) => {
      if (evento.key === 'Escape') setAbierto(false);
    };
    document.addEventListener('mousedown', cerrarFuera);
    document.addEventListener('keydown', cerrarEscape);
    return () => {
      document.removeEventListener('mousedown', cerrarFuera);
      document.removeEventListener('keydown', cerrarEscape);
    };
  }, [abierto]);

  return (
    <div ref={contenedor} className="position-relative">
      <label className="form-label" htmlFor={`combo-taller-${etiqueta.replace(/[^a-z0-9]/gi, '-')}`}>{etiqueta}</label>
      <button
        id={`combo-taller-${etiqueta.replace(/[^a-z0-9]/gi, '-')}`}
        type="button"
        className={`form-control text-start${error ? ' is-invalid' : ''}`}
        aria-haspopup="listbox"
        aria-expanded={abierto}
        onClick={() => { setFiltro(''); setAbierto((actual) => !actual); }}
      >
        {seleccionado ? seleccionado.nombre : 'Selecciona un taller'}
      </button>
      {error && <div className="invalid-feedback d-block">{error}</div>}
      {abierto && (
        <div className="position-absolute top-100 start-0 end-0 bg-white border rounded shadow p-2" style={{ zIndex: 1080 }}>
          <input
            autoFocus
            type="search"
            className="form-control form-control-sm mb-2"
            aria-label={`Filtrar ${etiqueta}`}
            placeholder="Filtrar talleres..."
            value={filtro}
            onChange={(evento) => setFiltro(evento.target.value)}
          />
          <div role="listbox" aria-label={etiqueta} className="overflow-auto" style={{ maxHeight: '14rem' }}>
            {visibles.map((t) => (
              <button
                key={t.id}
                type="button"
                role="option"
                aria-selected={String(t.id) === String(valor)}
                className="btn btn-light w-100 text-start mb-1"
                onClick={() => { onChange(String(t.id)); setAbierto(false); }}
              >
                {String(t.id) === String(valor) ? '✓ ' : ''}{t.nombre}
              </button>
            ))}
            {visibles.length === 0 && <div className="small text-muted p-2">No hay talleres que coincidan.</div>}
          </div>
        </div>
      )}
    </div>
  );
}

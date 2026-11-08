import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Layout from '../components/Layout.jsx';

export default function Documentacion() {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/documentacion')
      .then(setDatos)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <Layout titulo="Documentación técnica" subtitulo="Diagramas y bitácora del sistema">
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      {!datos && !error && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" aria-label="Cargando documentación" />
        </div>
      )}
      {datos && (
        <>
          <div className="alert alert-secondary small">
            Pantalla de administración. Los diagramas se sirven desde <code>/diagramas/</code> y la documentación por
            tabla desde <code>tablas/</code>.
          </div>
          <h2 className="h5 fw-bold mt-4 mb-3">Diagramas (SVG con fuente .md / .mmd)</h2>
          <div className="row g-4 mb-5">
            {(datos.diagramas || []).map((d) => (
              <div className="col-xl-6" key={d.archivo}>
                <div className="data-card shadow-sm h-100">
                  <div className="card-header d-flex justify-content-between align-items-center">
                    <h3 className="h6 fw-bold mb-0">{d.titulo}</h3>
                    <a className="btn btn-sm btn-outline-primary" href={`/diagramas/${d.archivo}`} target="_blank" rel="noreferrer">
                      Abrir SVG
                    </a>
                  </div>
                  <div className="p-3">
                    <a href={`/diagramas/${d.archivo}`} target="_blank" rel="noreferrer">
                      <img src={`/diagramas/${d.archivo}`} alt={d.titulo} className="img-diagrama" />
                    </a>
                    <p className="small text-muted mt-2 mb-0">Fuente: diagramas/{d.fuente}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <h2 className="h5 fw-bold mt-4 mb-3">Bitácora de fase</h2>
          <div className="data-card shadow-sm mb-4">
            <div className="card-header">
              <h3 className="h6 fw-bold mb-0">tabla_tallers.md</h3>
            </div>
            <div className="p-3">
              <ul className="mb-0">
                {(datos.seccionesBitacora || []).map((s) => (
                  <li key={s}>{s}</li>
                ))}
              </ul>
              <p className="small text-muted mt-2 mb-0">Archivo: tabla_tallers.md (raíz del proyecto).</p>
            </div>
          </div>
          <h2 className="h5 fw-bold mt-4 mb-3">Documentación por tabla</h2>
          <div className="data-card shadow-sm">
            <div className="card-header">
              <h3 className="h6 fw-bold mb-0">tablas/</h3>
            </div>
            <div className="p-3">
              {datos.tablas && datos.tablas.length > 0 ? (
                <ul className="mb-0">
                  {datos.tablas.map((t) => (
                    <li key={t}>
                      <code>tablas/{t}</code>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mb-0">La carpeta tablas/ no está disponible en este entorno.</p>
              )}
            </div>
          </div>
        </>
      )}
    </Layout>
  );
}

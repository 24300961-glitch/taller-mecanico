import { useAuth } from '../auth.jsx';
import Layout from '../components/Layout.jsx';

/** Renderiza el saludo y la insignia del rol autenticado.
 * @returns {JSX.Element} panel principal sin tarjetas ni accesos rápidos
 * @throws {never} no propaga errores
 */
export default function Dashboard() {
  const { me } = useAuth();
  const nombre = (me && me.fullName) || 'Usuario';
  const rol = (me && me.rol) || 'Sin rol';

  return (
    <Layout titulo="Panel principal">
      <div className="py-4">
        <h2 className="fw-bold mb-0" style={{ color: 'var(--primary)', fontSize: 'clamp(1.3rem, 3vw, 1.9rem)' }}>
          Hola, {nombre} <span className="role-badge">{rol}</span>
        </h2>
      </div>
    </Layout>
  );
}

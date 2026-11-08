import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, csrf, olvidarCsrf } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [me, setMe] = useState(null);
  const [cargando, setCargando] = useState(true);

  const refrescar = useCallback(async () => {
    try {
      // Sin redirección automática: en páginas públicas el 401 es normal (sin sesión).
      const data = await api('/api/auth/me', { redirigir401: false });
      setMe(data);
      return data;
    } catch (e) {
      setMe(null);
      return null;
    }
  }, []);

  useEffect(() => {
    refrescar().finally(() => setCargando(false));
  }, [refrescar]);

  const login = useCallback(async (email, password) => {
    const t = await csrf();
    const res = await fetch('/login', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', [t.header]: t.token },
      body: new URLSearchParams({ email, password }),
      redirect: 'follow',
    });
    const url = res.url || '';
    if (url.includes('/dashboard')) {
      olvidarCsrf();
      const data = await refrescar();
      return { ok: true, me: data };
    }
    if (url.includes('error=desactivada')) {
      return { ok: false, mensaje: 'Tu cuenta está desactivada. Contacta al administrador del taller.' };
    }
    return { ok: false, mensaje: 'Correo o contraseña incorrectos. Verifica tus datos e inténtalo de nuevo.' };
  }, [refrescar]);

  const logout = useCallback(async () => {
    try {
      const t = await csrf();
      await fetch('/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { [t.header]: t.token },
        redirect: 'follow',
      });
    } catch (e) {
      /* El cierre es local aunque falle la red */
    } finally {
      olvidarCsrf();
      setMe(null);
    }
  }, []);

  const tienePermiso = useCallback(
    (permiso) => !!me && Array.isArray(me.permisos) && me.permisos.includes(permiso),
    [me]
  );

  return (
    <AuthContext.Provider value={{ me, cargando, refrescar, login, logout, tienePermiso }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

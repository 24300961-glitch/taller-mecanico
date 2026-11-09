// Cloudflare Worker — entrada única del Taller Mecánico
// Frontend: Netlify · API: Render · Base de datos: MonsterASP
// Rutas que van a la API: /api/*, /storage/*, /logout y POST /login.
// Todo lo demás se sirve desde Netlify (SPA).

const FRONTEND = 'https://tallermecanicog2.netlify.app';
const BACKEND = 'https://taller-api-a8d9.onrender.com';

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const p = url.pathname;

    const vaAlBackend =
      p.startsWith('/api/') ||
      p.startsWith('/storage/') ||
      p === '/logout' ||
      (p === '/login' && request.method === 'POST');

    const destino = vaAlBackend ? BACKEND : FRONTEND;
    const nueva = new URL(url.pathname + url.search, destino);

    // Filtrar cabeceras conflictivas de Cloudflare (evita el Error 429 por CDN-Loop al llamar a Render)
    const headers = new Headers();
    for (const [key, value] of request.headers) {
      const k = key.toLowerCase();
      if (!k.startsWith('cf-') && k !== 'cdn-loop' && k !== 'host') {
        headers.set(key, value);
      }
    }
    headers.set('Host', nueva.host);
    headers.set('X-Forwarded-Host', url.host);

    const res = await fetch(nueva, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      redirect: 'manual',
    });

    // Las redirecciones del backend (/dashboard, /login?error=...) se devuelven
    // al dominio del Worker para que el navegador siga dentro de la SPA.
    const salida = new Headers(res.headers);
    const location = salida.get('location');
    if (location) {
      const base = new URL(BACKEND);
      const absoluta = new URL(location, base);
      if (absoluta.host === base.host) {
        salida.set('location', url.origin + absoluta.pathname + absoluta.search);
      }
    }

    return new Response(res.body, { status: res.status, headers: salida });
  },
};

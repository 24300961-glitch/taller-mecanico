// Cloudflare Worker — entrada única del Taller Mecánico
// Cambia estas dos URLs por las tuyas.

const FRONTEND = 'https://TU-SITIO.netlify.app';
const BACKEND = 'https://TU-API.onrender.com';

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

    const headers = new Headers(request.headers);
    headers.set('X-Forwarded-Host', url.host);

    const res = await fetch(nueva, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      redirect: 'manual',
    });

    const salida = new Headers(res.headers);
    const location = salida.get('location');
    if (location && location.startsWith(BACKEND)) {
      salida.set('location', location.replace(BACKEND, url.origin));
    }

    return new Response(res.body, { status: res.status, headers: salida });
  },
};

/* Cliente HTTP de la SPA. Réplica de window.TN.api del frontend anterior:
   mismo origen, sesión por cookie, CSRF por cabecera, errores homogéneos. */

export class ApiError extends Error {
  constructor(status, message, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data || {};
  }
}

const MENSAJES = {
  400: 'Los datos enviados no son válidos. Revisa el formulario.',
  401: 'Tu sesión expiró o no has iniciado sesión. Te llevaremos al inicio de sesión.',
  403: 'No tienes permiso para realizar esta acción.',
  404: 'El recurso solicitado no existe.',
  409: 'La operación entra en conflicto con datos existentes.',
  423: 'El recurso está bloqueado.',
  500: 'Ocurrió un error inesperado en el servidor. Intenta más tarde.',
  503: 'El servicio no está disponible en este momento. Intenta más tarde.',
};

let csrfCache = null;

export async function csrf() {
  if (csrfCache) return csrfCache;
  const res = await fetch('/api/auth/csrf', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
  const data = await res.json();
  csrfCache = data;
  return data;
}

export function olvidarCsrf() {
  csrfCache = null;
}

function irALogin() {
  setTimeout(() => {
    window.location.href = '/login?sesion=requerida';
  }, 2500);
}

export async function api(url, opciones) {
  opciones = opciones || {};
  const metodo = (opciones.method || 'GET').toUpperCase();
  const headers = { Accept: 'application/json' };
  const multipart = typeof FormData !== 'undefined' && opciones.body instanceof FormData;
  if (opciones.body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
  if (metodo !== 'GET') {
    const t = await csrf();
    if (t && t.token && t.header) headers[t.header] = t.token;
  }
  let res;
  try {
    res = await fetch(url, {
      method: metodo,
      headers,
      credentials: 'same-origin',
      body: opciones.body === undefined ? undefined : multipart ? opciones.body : JSON.stringify(opciones.body),
    });
  } catch (e) {
    throw new ApiError(0, 'No hay conexión con el servidor. Verifica tu red e inténtalo de nuevo.', {});
  }
  const tipo = res.headers.get('content-type') || '';
  let data = {};
  if (tipo.indexOf('application/json') !== -1) {
    try {
      data = await res.json();
    } catch (e) {
      data = {};
    }
  } else if (res.ok && res.redirected && res.url.indexOf('/login') !== -1) {
    irALogin();
    throw new ApiError(401, MENSAJES[401], {});
  }
  if (res.ok) return data;
  const mensaje = (data && data.message) || MENSAJES[res.status] || `Error inesperado (HTTP ${res.status}).`;
  if (res.status === 401 && opciones.redirigir401 !== false) irALogin();
  throw new ApiError(res.status, mensaje, data);
}

export function texto(valor, alterno) {
  return valor === null || valor === undefined || valor === '' ? alterno || '—' : String(valor);
}

export function telefonoLegible(t) {
  if (t && t.length === 10) return `${t.slice(0, 2)} ${t.slice(2, 6)} ${t.slice(6)}`;
  return texto(t);
}

export function fechaCorta(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
  } catch (e) {
    return iso;
  }
}

export function fechaCortaFecha(iso) {
  if (!iso) return 'Nunca';
  try {
    const d = new Date(iso);
    const p = (n) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
  } catch (e) {
    return iso;
  }
}

/* Reglas de validación replicadas del frontend anterior (los mensajes del
   backend mandan; estas evitan viajes inútiles al servidor). */
export const RX = {
  NOMBRE_80: /^[\p{L}][\p{L} .'\-]{1,79}$/u,
  NOMBRE_100: /^[\p{L}][\p{L} .'\-]{1,99}$/u,
  ROL: /^[\p{L}0-9][\p{L}0-9 _-]*$/u,
  TEL: /^[0-9 ()+\-]{10,20}$/,
  RFC: /^[A-ZÑ&]{3,4}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[A-Z0-9]{3}$/i,
  CP: /^\d{5}$/,
  PLACA: /^[A-Z0-9]{2,4}-[A-Z0-9]{2,4}(-[A-Z0-9]{2,4})?$/,
  VIN: /^[A-HJ-NPR-Z0-9]{17}$/i,
  MARCA: /^[\p{L}0-9 .&'\-]+$/u,
  MODELO: /^[\p{L}0-9 ./&'\-]+$/u,
  COLOR: /^[\p{L} \-]+$/u,
  ANIO: /^\d{4}$/,
};

export const digitos = (v) => (v || '').replace(/\D/g, '');

export function claveNormalizada(v) {
  return (v || '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

export function requisitosContrasena(p) {
  const faltan = [];
  p = p || '';
  if (p.length < 12) faltan.push('Al menos 12 caracteres');
  if (new TextEncoder().encode(p).length > 72) faltan.push('Máximo 72 caracteres');
  if (!/[A-ZÁÉÍÓÚÑ]/.test(p)) faltan.push('Una letra mayúscula');
  if (!/[a-záéíóúñ]/.test(p)) faltan.push('Una letra minúscula');
  if (!/\d/.test(p)) faltan.push('Un número');
  if (!/[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ\s]/.test(p)) faltan.push('Un símbolo (por ejemplo ! # $ % *)');
  if (p !== p.trim()) faltan.push('Sin espacios al inicio o al final');
  return faltan;
}

export const ANIO_MAXIMO = new Date().getFullYear() + 1;

(function () {
    'use strict';

    function meta(nombre) {
        var el = document.querySelector('meta[name="' + nombre + '"]');
        return el ? el.getAttribute('content') : null;
    }

    function ApiError(status, message, data) {
        this.name = 'ApiError';
        this.status = status;
        this.message = message;
        this.data = data || {};
    }
    ApiError.prototype = Object.create(Error.prototype);

    var MENSAJES = {
        400: 'Los datos enviados no son válidos. Revisa el formulario.',
        401: 'Tu sesión expiró o no has iniciado sesión. Te llevaremos al inicio de sesión.',
        403: 'No tienes permiso para realizar esta acción.',
        404: 'El recurso solicitado no existe.',
        409: 'La operación entra en conflicto con datos existentes.',
        423: 'El recurso está bloqueado.',
        500: 'Ocurrió un error inesperado en el servidor. Intenta más tarde.',
        503: 'El servicio no está disponible en este momento. Intenta más tarde.'
    };

    async function api(url, opciones) {
        opciones = opciones || {};
        var metodo = (opciones.method || 'GET').toUpperCase();
        var headers = { 'Accept': 'application/json' };
        var multipart = opciones.body instanceof FormData;
        if (opciones.body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
        var token = meta('_csrf');
        var header = meta('_csrf_header');
        if (token && header && metodo !== 'GET') headers[header] = token;
        var res;
        try {
            res = await fetch(url, {
                method: metodo,
                headers: headers,
                credentials: 'same-origin',
                body: opciones.body === undefined ? undefined : (multipart ? opciones.body : JSON.stringify(opciones.body))
            });
        } catch (e) {
            throw new ApiError(0, 'No hay conexión con el servidor. Verifica tu red e inténtalo de nuevo.', {});
        }
        var tipo = res.headers.get('content-type') || '';
        var data = {};
        if (tipo.indexOf('application/json') !== -1) {
            try { data = await res.json(); } catch (e) { data = {}; }
        } else if (res.ok && res.redirected && res.url.indexOf('/login') !== -1) {
            irALogin();
            throw new ApiError(401, MENSAJES[401], {});
        }
        if (res.ok) return data;
        var mensaje = (data && data.message) || MENSAJES[res.status] || ('Error inesperado (HTTP ' + res.status + ').');
        if (res.status === 401) irALogin();
        throw new ApiError(res.status, mensaje, data);
    }

    function irALogin() {
        setTimeout(function () { window.location.href = '/login?sesion=requerida'; }, 2500);
    }

    function alerta(contenedor, tipo, mensaje, detalles) {
        if (typeof contenedor === 'string') contenedor = document.getElementById(contenedor);
        if (!contenedor) return;
        contenedor.replaceChildren();
        if (!mensaje) return;
        var div = document.createElement('div');
        div.className = 'alert alert-' + tipo + ' alert-dismissible fade show mb-3';
        div.setAttribute('role', tipo === 'danger' ? 'alert' : 'status');
        var texto = document.createElement('div');
        texto.textContent = mensaje;
        div.appendChild(texto);
        if (detalles && detalles.length) {
            var ul = document.createElement('ul');
            ul.className = 'mb-0 mt-2 small';
            detalles.forEach(function (d) {
                var li = document.createElement('li');
                li.textContent = d;
                ul.appendChild(li);
            });
            div.appendChild(ul);
        }
        var cerrar = document.createElement('button');
        cerrar.type = 'button';
        cerrar.className = 'btn-close';
        cerrar.setAttribute('data-bs-dismiss', 'alert');
        cerrar.setAttribute('aria-label', 'Cerrar');
        div.appendChild(cerrar);
        contenedor.appendChild(div);
    }

    function alertaError(contenedor, error) {
        var detalles = [];
        if (error && error.data && error.data.errores) {
            Object.keys(error.data.errores).forEach(function (k) { detalles.push(error.data.errores[k]); });
        }
        alerta(contenedor, error && error.status === 409 ? 'warning' : 'danger',
            (error && error.message) || 'Ocurrió un error inesperado.', detalles);
    }

    function toast(mensaje, ok) {
        var zona = document.getElementById('tn-toasts');
        if (!zona) {
            zona = document.createElement('div');
            zona.id = 'tn-toasts';
            zona.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(zona);
        }
        var el = document.createElement('div');
        el.className = 'toast align-items-center text-white border-0 ' + (ok ? 'bg-success' : 'bg-danger');
        el.setAttribute('role', ok ? 'status' : 'alert');
        el.setAttribute('aria-live', 'assertive');
        var flex = document.createElement('div');
        flex.className = 'd-flex';
        var body = document.createElement('div');
        body.className = 'toast-body small fw-semibold';
        body.textContent = mensaje;
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-close btn-close-white me-2 m-auto';
        btn.setAttribute('data-bs-dismiss', 'toast');
        flex.appendChild(body);
        flex.appendChild(btn);
        el.appendChild(flex);
        zona.appendChild(el);
        var t = new bootstrap.Toast(el, { delay: ok ? 3500 : 6000 });
        el.addEventListener('hidden.bs.toast', function () { el.remove(); });
        t.show();
    }

    function feedbackDe(campo) {
        var fb = campo.parentElement.querySelector('.invalid-feedback');
        if (!fb) {
            fb = document.createElement('div');
            fb.className = 'invalid-feedback';
            campo.insertAdjacentElement('afterend', fb);
        }
        return fb;
    }

    function limpiarErrores(form) {
        form.querySelectorAll('.is-invalid').forEach(function (el) { el.classList.remove('is-invalid'); });
        form.querySelectorAll('[data-custom-error]').forEach(function (el) {
            el.setCustomValidity('');
            el.removeAttribute('data-custom-error');
        });
    }

    function marcarError(campo, mensaje) {
        if (!campo) return;
        campo.classList.add('is-invalid');
        feedbackDe(campo).textContent = mensaje;
    }

    function mostrarErrores(form, errores) {
        if (!errores) return;
        var primero = null;
        Object.keys(errores).forEach(function (nombre) {
            var campo = form.querySelector('[name="' + nombre + '"]');
            if (campo) {
                marcarError(campo, errores[nombre]);
                if (!primero) primero = campo;
            }
        });
        if (primero) primero.focus();
    }

    function validar(form, reglasExtra) {
        limpiarErrores(form);
        var errores = {};
        if (reglasExtra) {
            var extra = reglasExtra() || {};
            Object.keys(extra).forEach(function (k) {
                var campo = form.querySelector('[name="' + k + '"]');
                if (campo) {
                    campo.setCustomValidity(extra[k]);
                    campo.setAttribute('data-custom-error', '1');
                }
            });
        }
        form.querySelectorAll('input, select, textarea').forEach(function (campo) {
            if (!campo.name || campo.disabled) return;
            if (!campo.checkValidity()) {
                errores[campo.name] = campo.dataset.mensaje && !campo.hasAttribute('data-custom-error')
                    ? campo.dataset.mensaje : campo.validationMessage;
            }
        });
        mostrarErrores(form, errores);
        return Object.keys(errores).length === 0;
    }

    function ocupado(boton, activo, texto) {
        if (!boton) return;
        if (activo) {
            boton.dataset.textoOriginal = boton.textContent;
            boton.disabled = true;
            boton.replaceChildren();
            var s = document.createElement('span');
            s.className = 'spinner-border spinner-border-sm me-2';
            s.setAttribute('aria-hidden', 'true');
            boton.appendChild(s);
            boton.appendChild(document.createTextNode(texto || 'Procesando...'));
        } else {
            boton.disabled = false;
            boton.textContent = boton.dataset.textoOriginal || boton.textContent;
        }
    }

    function requisitosContrasena(p) {
        var faltan = [];
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

    function texto(valor, alterno) {
        return valor === null || valor === undefined || valor === '' ? (alterno || '—') : String(valor);
    }

    function celda(tr, contenido, clase) {
        var td = document.createElement('td');
        if (clase) td.className = clase;
        if (contenido instanceof Node) td.appendChild(contenido); else td.textContent = texto(contenido);
        tr.appendChild(td);
        return td;
    }

    function insignia(textoInsignia, clase) {
        var s = document.createElement('span');
        s.className = 'badge rounded-pill ' + clase;
        s.textContent = textoInsignia;
        return s;
    }

    function formulario(form) {
        var datos = {};
        form.querySelectorAll('input, select, textarea').forEach(function (c) {
            if (!c.name || c.disabled) return;
            if (c.type === 'checkbox') datos[c.name] = c.checked;
            else if (c.type === 'number') datos[c.name] = c.value === '' ? null : Number(c.value);
            else datos[c.name] = c.value.trim();
        });
        return datos;
    }

    document.addEventListener('input', function (e) {
        var c = e.target;
        if (c && c.classList && c.classList.contains('is-invalid')) {
            c.classList.remove('is-invalid');
            if (c.hasAttribute('data-custom-error')) {
                c.setCustomValidity('');
                c.removeAttribute('data-custom-error');
            }
        }
    });

    var ICONO_OJO = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">'
        + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M8 12.5A4.5 4.5 0 1 1 8 3a4.5 4.5 0 0 1 0 9M8 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4"/></svg>';
    var ICONO_OJO_CERRADO = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">'
        + '<path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755q-.247.248-.517.486zM11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829M3.35 5.47q-.27.24-.518.487A13 13 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7 7 0 0 1 8 13.5C2.96 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12z"/></svg>';

    /** Agrega un botón de ojo a cada campo de contraseña para ver u ocultar su valor.
     * @param raiz contenedor opcional; document cuando se omite
     * @return No aplica; los campos reciben el botón en línea.
     */
    function ojoContrasenas(raiz) {
        var scope = raiz || document;
        var inputs = scope.querySelectorAll('input[type="password"]');
        Array.prototype.forEach.call(inputs, function (inp) {
            if (inp.dataset.ojo === '1') return;
            inp.dataset.ojo = '1';
            var grupo = inp.parentNode && inp.parentNode.classList && inp.parentNode.classList.contains('input-group')
                ? inp.parentNode : null;
            if (!grupo) {
                grupo = document.createElement('div');
                grupo.className = 'input-group';
                inp.parentNode.insertBefore(grupo, inp);
                grupo.appendChild(inp);
            }
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-outline-secondary';
            btn.setAttribute('aria-label', 'Mostrar contraseña');
            btn.setAttribute('title', 'Mostrar contraseña');
            btn.innerHTML = ICONO_OJO;
            btn.addEventListener('click', function () {
                var visible = inp.type === 'password';
                inp.type = visible ? 'text' : 'password';
                btn.innerHTML = visible ? ICONO_OJO_CERRADO : ICONO_OJO;
                btn.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
                btn.setAttribute('title', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
                inp.focus();
            });
            grupo.appendChild(btn);
        });
    }

    document.addEventListener('DOMContentLoaded', function () { ojoContrasenas(document); });

    window.TN = {
        api: api, ApiError: ApiError, alerta: alerta, alertaError: alertaError, toast: toast,
        limpiarErrores: limpiarErrores, mostrarErrores: mostrarErrores, marcarError: marcarError,
        validar: validar, ocupado: ocupado, requisitosContrasena: requisitosContrasena,
        texto: texto, celda: celda, insignia: insignia, formulario: formulario,
        ojoContrasenas: ojoContrasenas
    };
})();

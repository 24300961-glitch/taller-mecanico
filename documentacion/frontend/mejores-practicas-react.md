# Mejores prácticas — React (Taller Mecánico)

Reglas de calidad del frontend. Sirven para revisar código y para que el `documentador-frontend` sepa qué reflejar.

## 1. Organización

- `pages/` = una página por ruta. Orquestan componentes y llaman a la API.
- `components/` = piezas reutilizables sin conocimiento de la ruta.
- `api.js` = **único** lugar con `fetch`. Ningún componente llama a `fetch` directamente.
- `auth.jsx` = contexto de sesión y permisos.
- `App.jsx` = rutas y protección por rol.
- Un archivo nuevo solo si tiene contenido real. No crear carpetas vacías.

## 2. Componentes

- Una responsabilidad por componente.
- Props explícitas; sin pasar objetos gigantes si solo se usa un campo.
- Sin lógica de negocio compleja dentro del JSX.
- Extraer a componente cuando se repite 3 veces o supera ~150 líneas.
- Nombres en PascalCase para componentes, camelCase para funciones y variables.

## 3. Estado

- Estado local con `useState` para lo que solo usa ese componente.
- Contexto para lo global (sesión, permisos).
- No duplicar estado que se puede derivar.
- No guardar en el cliente lo que el servidor ya devuelve.

## 4. Datos y API

- Todas las llamadas pasan por `api.js` (incluye CSRF y `credentials`).
- Manejar siempre tres estados: **cargando**, **error**, **datos**.
- El servidor pagina y ordena; el cliente no ordena listas grandes.
- Mensajes de error del backend al usuario, sin códigos técnicos.
- Nunca poner URLs absolutas del backend: usar rutas relativas (`/api/...`) para que el proxy de publicación funcione.

## 5. Formularios

- Validar en el cliente para dar feedback rápido, y en el servidor como fuente de verdad.
- Deshabilitar el botón mientras se envía.
- Mostrar el error del campo junto al campo.
- No borrar lo que el usuario escribió si falla el envío.

## 6. Seguridad

- Rutas protegidas por rol en `App.jsx`; el backend vuelve a validar.
- No guardar tokens ni contraseñas en `localStorage`.
- No mostrar información de otros roles.
- Confirmar acciones destructivas.

## 7. Accesibilidad y UX

- Todo control con `label` o `aria-label`.
- Navegación por teclado en modales y tablas.
- Foco visible.
- Contraste suficiente; no depender solo del color para indicar estado.
- Responsive: tablas con scroll horizontal en pantallas pequeñas.

## 8. Rendimiento

- `useMemo`/`useCallback` solo cuando hay un costo medible.
- Evitar renderizar listas completas si hay paginación.
- Imágenes con tamaño controlado.
- No bloquear la UI con cálculos pesados.

## 9. Estilo y consistencia

- Un solo sistema de estilos (`styles.css` + clases del proyecto).
- Sin estilos inline salvo casos puntuales.
- Textos en español, consistentes en toda la app.
- Mensajes con el mismo tono (claro, directo).

## 10. Pruebas

- Pruebas de componentes con React Testing Library.
- Probar el flujo feliz y al menos un error por formulario.
- No probar detalles de implementación (clases CSS, estado interno).

## 11. Checklist antes de entregar

- [ ] Compila (`npm run build`).
- [ ] Sin llamadas `fetch` fuera de `api.js`.
- [ ] Estados de carga y error visibles.
- [ ] Rutas protegidas por rol.
- [ ] Sin URLs absolutas del backend.
- [ ] Funciona en pantalla pequeña.
- [ ] Sin `console.log` de depuración.

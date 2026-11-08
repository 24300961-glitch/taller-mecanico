# Casos de uso: talleres y clientes

## Registrar taller

1. El Administrador abre `/talleres` y captura nombre, RFC, email, teléfono, dirección y estatus; la foto es opcional.
2. Al completar un CP de cinco dígitos, el formulario consulta el catálogo SEPOMEX local. Si existen varias colonias, se proponen y el Administrador puede corregir colonia, municipio o estado.
3. El servidor normaliza textos a minúsculas, conserva el RFC en mayúsculas y valida campos y foto antes de insertar.
4. Si coincide RFC o nombre más domicilio con un taller existente, responde conflicto y el front muestra el registro existente. Si no, confirma los datos insertados.
5. El registro de foto, cuando se adjunta, se guarda en `storage/talleres`; la tabla conserva la ruta relativa.

## Cargar catálogo SEPOMEX

1. Descarga el Catálogo Nacional en la página oficial de [Correos de México](https://www.correosdemexico.gob.mx/sslservicios/consultacp/CodigoPostal_Exportar.aspx). Para el seeder usa TXT delimitado por `|` o CSV con encabezados reconocibles (`d_codigo`, `d_asenta`, `D_mnpio` y `d_estado`). La [descripción oficial de campos](https://www.correosdemexico.gob.mx/SSLServicios/ConsultaCP/imagenes/Descrip.pdf) identifica código postal, asentamiento, municipio y estado.
2. Coloca el archivo descargado dentro de `data/sepomex/` cuando uses Docker Compose; esa carpeta se monta de solo lectura en `/app/data/sepomex`.
3. En `.env`, configura `SEPOMEX_FILE` con la ruta real dentro del contenedor y activa `SEPOMEX_IMPORTAR=true` para la primera carga.
4. Inicia la aplicación. El seeder carga los campos requeridos a `catalogo_codigos_postales`, normaliza texto a minúsculas y omite filas duplicadas dentro del archivo. Si ya hay filas, omite la carga para no duplicarlas.
5. Después de la carga, vuelve `SEPOMEX_IMPORTAR=false`. No se incluye ni inventa información postal en el repositorio.

## Registrar o reutilizar cliente

1. Administrador o Recepcionista abre el alta solo cuando existe al menos un taller activo.
2. Captura cliente por email y selecciona uno o más talleres activos.
3. Si el email no existe, inserta un cliente y asocia los talleres seleccionados.
4. Si el email existe, no inserta otro cliente ni modifica sus datos; reutiliza el registro y agrega las asociaciones seleccionadas.

## Registrar o cambiar taller de un vehículo

1. Administrador o Recepcionista captura propietario y datos del vehículo.
2. Selecciona un taller activo específico. Esa asignación es independiente de los talleres asociados al cliente.
3. El alta guarda el propietario y el taller en el vehículo. Al cambiar de ubicación, se exige el nuevo taller activo.

## Consultar taller y propietario

1. Administrador y Recepcionista consultan la lista de vehículos con propietario y taller asignado.
2. El Cliente autenticado consulta en su dashboard los vehículos cuyo correo de propietario coincide con el correo de su cuenta y ve el taller asignado a cada coche.

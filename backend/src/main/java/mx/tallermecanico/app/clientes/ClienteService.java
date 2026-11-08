package mx.tallermecanico.app.clientes;

import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.error.NegocioException;
import mx.tallermecanico.app.sepomex.CodigoPostal;
import mx.tallermecanico.app.sepomex.CodigoPostalRepository;
import mx.tallermecanico.app.taller.Taller;
import mx.tallermecanico.app.taller.TallerService;
import mx.tallermecanico.app.vehiculos.Vehiculo;
import mx.tallermecanico.app.vehiculos.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);
    private static final int MAX_RESULTADOS = 50;

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final TallerService taller;
    private final AuditService audit;
    private final CodigoPostalRepository sepomex;

    /** Construye el servicio de clientes y sus asociaciones existentes.
     * @param clientes repositorio de personas
     * @param vehiculos repositorio de vehículos
     * @param taller servicio para validar talleres elegidos
     * @param audit registro de acciones
     * @param sepomex catálogo local de códigos postales
     */
    public ClienteService(ClienteRepository clientes, VehiculoRepository vehiculos, TallerService taller,
                          AuditService audit, CodigoPostalRepository sepomex) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.taller = taller;
        this.audit = audit;
        this.sepomex = sepomex;
    }

    public record Coincidencia(Long id, String nombreCompleto, String telefono, String telefonoAlterno, String email,
                               String rfc, boolean activo, List<String> motivos, boolean bloqueante) {}

    @Transactional(readOnly = true)
    /** Devuelve talleres activos disponibles para una asociación nueva.
     * @return talleres activos ordenados por nombre
     */
    public List<Taller> talleresActivos() {
        return taller.consultar(false);
    }

    /** Valida teléfonos/email y normaliza textos de cliente a minúsculas salvo RFC.
     * @param r formulario recibido
     * @return datos limpios para guardar
     * @throws NegocioException si teléfono o email no son válidos
     */
    public DatosCliente normalizar(ClienteRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        String telefono = Texto.soloDigitos(r.telefono());
        String telAlt = Texto.soloDigitos(r.telefonoAlterno());
        String telPer = Texto.soloDigitos(r.telefonoPersonal());
        String telTra = Texto.soloDigitos(r.telefonoTrabajo());
        String telCon = Texto.soloDigitos(r.telefonoContacto());
        if (telefono == null || telefono.length() != 10) {
            errores.put("telefono", "El teléfono debe tener exactamente 10 dígitos");
        }
        validarTelefono(errores, "telefonoAlterno", "El teléfono alterno debe tener exactamente 10 dígitos", r.telefonoAlterno(), telAlt);
        validarTelefono(errores, "telefonoPersonal", "El teléfono personal debe tener exactamente 10 dígitos", r.telefonoPersonal(), telPer);
        validarTelefono(errores, "telefonoTrabajo", "El teléfono de trabajo debe tener exactamente 10 dígitos", r.telefonoTrabajo(), telTra);
        validarTelefono(errores, "telefonoContacto", "El teléfono del contacto debe tener exactamente 10 dígitos", r.telefonoContacto(), telCon);
        java.util.Set<String> distintos = new java.util.LinkedHashSet<>();
        for (String t : new String[]{telefono, telAlt, telPer, telTra, telCon}) {
            if (t != null && !distintos.add(t)) {
                errores.put("telefonoContacto", "Los teléfonos del cliente deben ser diferentes entre sí");
                break;
            }
        }
        String nombreContacto = Texto.limpiar(r.nombreContacto());
        if ((telCon == null) != (nombreContacto == null)) {
            errores.put("nombreContacto", "El contacto alternativo requiere nombre y teléfono juntos");
            errores.put("telefonoContacto", "El contacto alternativo requiere nombre y teléfono juntos");
        }
        String rfc = Texto.limpiar(r.rfc());
        if (Texto.correo(r.email()) == null) errores.put("email", "El correo electrónico es obligatorio para identificar al cliente");
        if (!errores.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Revisa los campos marcados: hay datos inválidos o incompletos.",
                    Map.of("errores", errores));
        }
        DatosCliente d = new DatosCliente(
                minusculas(r.nombre()),
                minusculas(r.apellidoPaterno()),
                minusculas(r.apellidoMaterno()),
                telefono,
                telAlt,
                telPer,
                telTra,
                minusculas(nombreContacto),
                telCon,
                Texto.correo(r.email()),
                rfc == null ? null : rfc.toUpperCase(),
                minusculas(r.direccion()),
                minusculas(r.calle()),
                minusculas(r.numero()),
                minusculas(r.colonia()),
                Texto.limpiar(r.codigoPostal()),
                minusculas(r.municipio()),
                minusculas(r.estado()));
        validarCpEstado(d, errores);
        if (!errores.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Revisa los campos marcados: hay datos inválidos o incompletos.",
                    Map.of("errores", errores));
        }
        return d;
    }

    /** Valida un teléfono opcional de 10 dígitos.
     * @param errores mapa de errores acumulado
     * @param campo nombre del campo
     * @param mensaje mensaje cuando el formato no cumple
     * @param crudo valor recibido del formulario
     * @param digitos dígitos extraídos o null
     */
    private static void validarTelefono(Map<String, String> errores, String campo, String mensaje, String crudo, String digitos) {
        if (crudo != null && !crudo.isBlank() && (digitos == null || digitos.length() != 10)) {
            errores.put(campo, mensaje);
        }
    }

    /** Valida que el código postal pertenezca al estado indicado según el catálogo SEPOMEX.
     * Si el CP no existe en el catálogo local, los datos se aceptan para corrección manual.
     * @param d datos normalizados del cliente
     * @param errores mapa de errores acumulado
     */
    private void validarCpEstado(DatosCliente d, Map<String, String> errores) {
        if (d.codigoPostal() == null || d.estado() == null) return;
        java.util.List<CodigoPostal> opciones = sepomex.findByCodigoPostalOrderByEstadoAscMunicipioAscColoniaAsc(d.codigoPostal());
        if (opciones.isEmpty()) return;
        String esperado = Texto.normalizar(d.estado());
        boolean coincide = opciones.stream().map(o -> Texto.normalizar(o.getEstado())).anyMatch(esperado::equals);
        if (!coincide) {
            errores.put("estado", "El CP " + d.codigoPostal() + " no pertenece a este estado según SEPOMEX.");
            errores.put("codigoPostal", "Revisa que el código postal corresponda al estado.");
        }
    }

    @Transactional(readOnly = true)
    /** Busca un duplicado exacto únicamente por email.
     * @param d datos normalizados
     * @param excluirId cliente omitido al editar
     * @return lista vacía o la coincidencia que bloquea el alta
     */
    public List<Coincidencia> buscarCoincidencias(DatosCliente d, Long excluirId) {
        return clientes.findFirstByEmailIgnoreCase(d.email()).filter(c -> !Objects.equals(c.getId(), excluirId))
                .map(c -> List.of(new Coincidencia(c.getId(), c.getNombreCompleto(), c.getTelefono(),
                        c.getTelefonoAlterno(), c.getEmail(), c.getRfc(), c.isActivo(),
                        List.of("Mismo correo electrónico"), true))).orElseGet(List::of);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    /** Inserta un cliente nuevo y asocia uno o más talleres activos.
     * @param r datos del cliente y talleres seleccionados
     * @return cliente persistido
     * @throws NegocioException 409 si el correo existe; 400 si no hay talleres o los datos fallan
     */
    public Cliente registrar(ClienteRequest r) {
        DatosCliente d = normalizar(r);
        List<Taller> seleccionados = talleresSeleccionados(r.tallerIds());
        Cliente existente = clientes.findFirstByEmailIgnoreCase(d.email()).orElse(null);
        if (existente != null) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Ya existe un cliente con el correo electrónico " + existente.getEmail() + ". Elimina o reactiva el registro anterior.",
                    Map.of("codigo", "CLIENTE_DUPLICADO", "clienteExistente", Map.of("id", existente.getId(), "nombreCompleto", existente.getNombreCompleto())));
        }
        Cliente c = new Cliente(d);
        c.asociarTalleres(seleccionados);
        c = clientes.save(c);
        audit.record("CLIENTE", c.getId(), "CLIENTE_REGISTRADO",
                "email=" + c.getEmail() + " talleres=" + seleccionados.stream().map(Taller::getId).toList());
        log.info("[Clientes] Cliente {} registrado por {}", c.getId(), usuarioActual());
        return c;
    }

    /** Rechaza el alta o la edición cuando otro cliente ya usa el correo, RFC,
     * nombre completo o alguno de los teléfonos.
     * @param d datos normalizados
     * @param excluirId cliente omitido al editar; null al registrar
     * @throws NegocioException 409 con el detalle de la coincidencia
     */
    private void rechazarDuplicados(DatosCliente d, Long excluirId) {
        String nombreNorm = Texto.normalizar(
                (d.nombre() == null ? "" : d.nombre()) + " " + (d.apellidoPaterno() == null ? "" : d.apellidoPaterno())
                        + " " + (d.apellidoMaterno() == null ? "" : d.apellidoMaterno()));
        List<Cliente> cruces = clientes.buscarCoincidencias(excluirId, d.email(), d.rfc(), nombreNorm,
                d.telefono(), d.telefonoAlterno(), d.telefonoPersonal(), d.telefonoTrabajo(), d.telefonoContacto());
        if (cruces.isEmpty()) return;
        Cliente otro = cruces.get(0);
        List<String> motivos = new ArrayList<>();
        if (d.email() != null && d.email().equalsIgnoreCase(otro.getEmail())) motivos.add("Mismo correo electrónico");
        if (d.rfc() != null && d.rfc().equals(otro.getRfc())) motivos.add("Mismo RFC");
        if (nombreNorm != null && nombreNorm.equals(otro.getNombreNormalizado())) motivos.add("Mismo nombre completo");
        if (motivos.isEmpty()) motivos.add("Mismo teléfono");
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("codigo", "CLIENTE_DUPLICADO");
        detalle.put("clienteExistente", Map.of("id", otro.getId(), "nombreCompleto", otro.getNombreCompleto(),
                "email", otro.getEmail() == null ? "" : otro.getEmail(), "activo", otro.isActivo()));
        throw new NegocioException(HttpStatus.CONFLICT,
                "Ya existe el cliente " + otro.getNombreCompleto() + " (" + String.join(", ", motivos) + ").",
                detalle);
    }

    @Transactional(readOnly = true)
    /** Busca clientes por nombre, correo, teléfono o ID.
     * @param q texto de búsqueda opcional
     * @param incluirInactivos indica si incluye clientes desactivados
     * @return hasta el máximo de resultados configurado
     */
    public List<Cliente> consultar(String q, boolean incluirInactivos) {
        String limpio = Texto.limpiar(q);
        PageRequest page = PageRequest.of(0, MAX_RESULTADOS);
        if (limpio == null) {
            return clientes.consultar(null, null, null, null, incluirInactivos, page);
        }
        String texto = "%" + Texto.normalizar(limpio) + "%";
        String correo = "%" + limpio.toLowerCase() + "%";
        String digitosQ = Texto.soloDigitos(limpio);
        String digitos = digitosQ != null && digitosQ.length() >= 3 ? "%" + digitosQ + "%" : null;
        Long id = null;
        if (limpio.matches("^#?\\d{1,18}$")) {
            id = Long.parseLong(limpio.replace("#", ""));
        }
        return clientes.consultar(texto, correo, digitos, id, incluirInactivos, page);
    }

    @Transactional(readOnly = true)
    /** Pagina clientes por nombre, correo, teléfono o ID.
     * @param q texto de búsqueda opcional
     * @param incluirInactivos indica si incluye clientes desactivados
     * @param pagina número de página base cero
     * @param tamano filas por página entre 5 y 100
     * @return página de clientes ordenada por estatus y nombre
     */
    public Page<Cliente> consultarPagina(String q, boolean incluirInactivos, int pagina, int tamano) {
        int p = Math.max(0, pagina);
        int t = Math.min(100, Math.max(5, tamano));
        String limpio = Texto.limpiar(q);
        String texto = null, correo = null, digitos = null;
        Long id = null;
        if (limpio != null) {
            texto = "%" + Texto.normalizar(limpio) + "%";
            correo = "%" + limpio.toLowerCase() + "%";
            String digitosQ = Texto.soloDigitos(limpio);
            digitos = digitosQ != null && digitosQ.length() >= 3 ? "%" + digitosQ + "%" : null;
            if (limpio.matches("^#?\\d{1,18}$")) id = Long.parseLong(limpio.replace("#", ""));
        }
        PageRequest page = PageRequest.of(p, t);
        List<Cliente> contenido = clientes.consultar(texto, correo, digitos, id, incluirInactivos, page);
        long total = clientes.contar(texto, correo, digitos, id, incluirInactivos);
        return new PageImpl<>(contenido, page, total);
    }

    @Transactional(readOnly = true)
    /** Recupera un cliente por identificador.
     * @param id identificador solicitado
     * @return cliente persistido
     * @throws NegocioException 404 si no existe
     */
    public Cliente obtener(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un cliente con ID " + id));
    }

    /** Carga el cliente con sus talleres asociados.
     * @param id identificador del cliente
     * @return entidad con asociaciones inicializadas
     * @throws NegocioException 404 si no existe
     */
    @Transactional(readOnly = true)
    public Cliente obtenerConTalleres(Long id) {
        return clientes.findWithTalleresById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                "No existe un cliente con ID " + id));
    }

    @Transactional(readOnly = true)
    /** Lista vehículos del propietario seleccionado.
     * @param clienteId identificador de cliente
     * @return vehículos ordenados por estatus y placa
     */
    public List<Vehiculo> vehiculosDe(Long clienteId) {
        return vehiculos.findByClienteIdOrderByActivoDescPlacaAsc(clienteId);
    }

    @Transactional
    /** Actualiza datos personales manteniendo talleres asociados.
     * @param id cliente existente
     * @param r datos nuevos
     * @return cliente actualizado
     * @throws NegocioException si email ya pertenece a otro cliente o datos fallan
     */
    public Cliente actualizar(Long id, ClienteRequest r) {
        Cliente c = obtener(id);
        if (!c.isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El cliente está desactivado. Reactívalo antes de corregir sus datos.");
        }
        DatosCliente d = normalizar(r);
        rechazarDuplicados(d, id);
        String cambios = diferencias(c, d);
        if (cambios.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No hay cambios que guardar.");
        }
        c.aplicar(d);
        clientes.save(c);
        audit.record("CLIENTE", c.getId(), "CLIENTE_ACTUALIZADO", cambios);
        return c;
    }

    private static String diferencias(Cliente c, DatosCliente d) {
        List<String> cambios = new ArrayList<>();
        comparar(cambios, "nombre", c.getNombre(), d.nombre());
        comparar(cambios, "apellidoPaterno", c.getApellidoPaterno(), d.apellidoPaterno());
        comparar(cambios, "apellidoMaterno", c.getApellidoMaterno(), d.apellidoMaterno());
        comparar(cambios, "telefono", c.getTelefono(), d.telefono());
        comparar(cambios, "telefonoAlterno", c.getTelefonoAlterno(), d.telefonoAlterno());
        comparar(cambios, "telefonoPersonal", c.getTelefonoPersonal(), d.telefonoPersonal());
        comparar(cambios, "telefonoTrabajo", c.getTelefonoTrabajo(), d.telefonoTrabajo());
        comparar(cambios, "nombreContacto", c.getNombreContacto(), d.nombreContacto());
        comparar(cambios, "telefonoContacto", c.getTelefonoContacto(), d.telefonoContacto());
        comparar(cambios, "email", c.getEmail(), d.email());
        comparar(cambios, "rfc", c.getRfc(), d.rfc());
        comparar(cambios, "direccion", c.getDireccion(), d.direccion());
        comparar(cambios, "calle", c.getCalle(), d.calle());
        comparar(cambios, "numero", c.getNumero(), d.numero());
        comparar(cambios, "colonia", c.getColonia(), d.colonia());
        comparar(cambios, "codigoPostal", c.getCodigoPostal(), d.codigoPostal());
        comparar(cambios, "municipio", c.getMunicipio(), d.municipio());
        comparar(cambios, "estado", c.getEstado(), d.estado());
        return String.join("; ", cambios);
    }

    private static void comparar(List<String> cambios, String campo, String antes, String despues) {
        if (!Objects.equals(antes, despues)) cambios.add(campo + ": '" + (antes == null ? "" : antes) + "' -> '" + (despues == null ? "" : despues) + "'");
    }

    @Transactional
    /** Desactiva una persona sin borrar sus vehículos ni talleres.
     * @param id cliente solicitado
     * @param motivo motivo de desactivación
     * @return cliente actualizado
     * @throws NegocioException si no existe o ya está inactivo
     */
    public Cliente desactivar(Long id, String motivo) {
        Cliente c = obtener(id);
        if (!c.isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El cliente ya está desactivado.");
        }
        c.desactivar(Texto.limpiar(motivo), usuarioActual());
        clientes.save(c);
        audit.record("CLIENTE", c.getId(), "CLIENTE_DESACTIVADO", "motivo=" + c.getMotivoDesactivacion());
        return c;
    }

    @Transactional
    /** Reactiva al cliente si su email no está usado por otro registro.
     * @param id cliente solicitado
     * @return cliente activo
     * @throws NegocioException si no existe, ya está activo o el email está duplicado
     */
    public Cliente reactivar(Long id) {
        Cliente c = obtener(id);
        if (c.isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El cliente ya está activo.");
        }
        DatosCliente d = new DatosCliente(c.getNombre(), c.getApellidoPaterno(), c.getApellidoMaterno(),
                c.getTelefono(), c.getTelefonoAlterno(), c.getTelefonoPersonal(), c.getTelefonoTrabajo(),
                c.getNombreContacto(), c.getTelefonoContacto(), c.getEmail(), c.getRfc(), c.getDireccion(),
                c.getCalle(), c.getNumero(), c.getColonia(), c.getCodigoPostal(), c.getMunicipio(), c.getEstado());
        if (d.email() != null && clientes.findFirstByEmailIgnoreCase(d.email()).filter(x -> !Objects.equals(x.getId(), id)).isPresent())
            throw new NegocioException(HttpStatus.CONFLICT, "Otro cliente ya usa ese correo electrónico.");
        c.reactivar();
        clientes.save(c);
        audit.record("CLIENTE", c.getId(), "CLIENTE_REACTIVADO", null);
        return c;
    }

    private static String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    /** Resuelve y valida talleres activos elegidos en el formulario.
     * @param tallerIds identificadores solicitados
     * @return talleres activos sin repetir
     * @throws NegocioException 400 si la selección está vacía, no existe o incluye un taller inactivo
     */
    private List<Taller> talleresSeleccionados(List<Long> tallerIds) {
        if (tallerIds == null || tallerIds.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Selecciona al menos un taller para asociar al cliente.",
                    Map.of("errores", Map.of("tallerIds", "Selecciona uno o más talleres activos.")));
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(tallerIds);
        if (ids.contains(null)) throw new NegocioException(HttpStatus.BAD_REQUEST,
                "La selección de talleres contiene un identificador vacío.",
                Map.of("errores", Map.of("tallerIds", "Selecciona talleres válidos.")));
        List<Taller> seleccionados = ids.stream().map(taller::obtenerActivo).toList();
        return seleccionados;
    }

    /** Normaliza texto de cliente a minúsculas.
     * @param value texto recibido
     * @return texto limpio en minúsculas o null
     */
    private static String minusculas(String value) {
        String limpio = Texto.limpiar(value);
        return limpio == null ? null : limpio.toLowerCase(java.util.Locale.ROOT);
    }
}

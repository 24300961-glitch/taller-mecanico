package mx.tallermecanico.app.vehiculos;

import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.clientes.Cliente;
import mx.tallermecanico.app.clientes.ClienteRepository;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.error.NegocioException;
import mx.tallermecanico.app.taller.TallerService;
import mx.tallermecanico.app.taller.Taller;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class VehiculoService {

    private static final int MAX_RESULTADOS = 50;

    private final VehiculoRepository vehiculos;
    private final ClienteRepository clientes;
    private final TallerService taller;
    private final AuditService audit;
    private final HistorialVehiculoRepository historial;

    public VehiculoService(VehiculoRepository vehiculos, ClienteRepository clientes, TallerService taller,
                           AuditService audit, HistorialVehiculoRepository historial) {
        this.vehiculos = vehiculos;
        this.clientes = clientes;
        this.taller = taller;
        this.audit = audit;
        this.historial = historial;
    }

    public record Coincidencia(Long id, String placa, String vin, String descripcion, Long clienteId,
                               String cliente, boolean activo, List<String> motivos) {}

    /** Valida y normaliza los datos del vehículo recibidos.
     * @param r datos del formulario
     * @return datos consistentes para persistencia
     * @throws NegocioException si placa, marca, modelo o año son inválidos
     */
    public DatosVehiculo normalizar(VehiculoRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        String placa = Texto.placa(r.placa());
        if (placa == null || !placa.matches("^[A-Z0-9]{2,4}-[A-Z0-9]{2,4}(-[A-Z0-9]{2,4})?$")) {
            errores.put("placa", "La placa debe ir en mayúsculas y separada por un guion, por ejemplo ABC-1234");
        }
        String marca = Texto.limpiar(r.marca());
        String modelo = Texto.limpiar(r.modelo());
        String color = Texto.limpiar(r.color());
        if (marca == null || !marca.matches("^[\\p{L}0-9 .&'-]{2,40}$"))
            errores.put("marca", "La marca debe tener entre 2 y 40 caracteres válidos.");
        if (modelo == null || !modelo.matches("^[\\p{L}0-9 ./&'-]{1,60}$"))
            errores.put("modelo", "El modelo debe tener hasta 60 caracteres válidos.");
        if (color != null && !color.matches("^[\\p{L} -]{1,30}$"))
            errores.put("color", "El color solo puede contener letras y espacios.");
        int maxAnio = Year.now().getValue() + 1;
        if (r.anio() == null || !String.valueOf(r.anio()).matches("^\\d{4}$") || r.anio() < 1900 || r.anio() > maxAnio) {
            errores.put("anio", "El año debe ser un número de 4 dígitos entre 1900 y " + maxAnio);
        }
        String vin = Texto.alfanumericoMayusculas(r.vin());
        if (vin != null && !vin.matches("[A-HJ-NPR-Z0-9]{17}"))
            errores.put("vin", "El VIN debe tener 17 caracteres alfanuméricos (sin I, O ni Q).");
        if (r.kilometraje() != null && r.kilometraje() < 0)
            errores.put("kilometraje", "El kilometraje no puede ser negativo.");
        if (r.version() != null && r.version().length() > 30)
            errores.put("version", "La versión no debe exceder 30 caracteres.");
        if (r.fotos() != null && r.fotos().length() > 500)
            errores.put("fotos", "Las fotos no deben exceder 500 caracteres.");
        if (!errores.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Revisa los campos marcados: hay datos inválidos o incompletos.",
                    Map.of("errores", errores));
        }
        return new DatosVehiculo(marca, modelo, r.anio(), placa, vin, color, Texto.limpiar(r.version()), r.kilometraje(), Texto.limpiar(r.fotos()));
    }

    /** Busca conflictos por placa o VIN antes de persistir cambios.
     * @param d datos normalizados
     * @param excluirId identificador excluido al editar; puede ser null
     * @return coincidencias con propietario y motivo
     */
    @Transactional(readOnly = true)
    public List<Coincidencia> buscarCoincidencias(DatosVehiculo d, Long excluirId) {
        return vehiculos.buscarCoincidencias(excluirId, d.placa(), d.vin()).stream().map(v -> {
            List<String> motivos = new ArrayList<>();
            if (v.getPlaca().equals(d.placa())) motivos.add("Misma placa");
            if (d.vin() != null && d.vin().equals(v.getVin())) motivos.add("Mismo VIN");
            return new Coincidencia(v.getId(), v.getPlaca(), v.getVin(),
                    v.getMarca() + " " + v.getModelo() + " " + v.getAnio(), v.getCliente().getId(),
                    v.getCliente().getNombreCompleto(), v.isActivo(), motivos);
        }).toList();
    }

    /** Rechaza la operación cuando existe placa o VIN coincidente.
     * @param coincidencias resultados encontrados
     * @return No aplica; continúa solo si la lista está vacía.
     * @throws NegocioException 409 cuando hay duplicados
     */
    private void rechazarDuplicados(List<Coincidencia> coincidencias) {
        if (coincidencias.isEmpty()) return;
        boolean inactivo = coincidencias.stream().anyMatch(c -> !c.activo());
        throw new NegocioException(HttpStatus.CONFLICT,
                "Ya existe un vehículo registrado con la misma placa o VIN"
                        + (inactivo ? " (está desactivado; puedes reactivarlo)." : "."),
                Map.of("codigo", "DUPLICADO", "coincidencias", coincidencias));
    }

    /** Busca y valida un cliente seleccionable para el vehículo.
     * @param id identificador del cliente
     * @return cliente existente y activo
     * @throws NegocioException 404 si no existe y 409 si está inactivo
     */
    private Cliente clienteActivo(Long id) {
        Cliente c = clientes.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un cliente con ID " + id));
        if (!c.isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El cliente " + c.getNombreCompleto()
                    + " está desactivado. Reactívalo antes de asociarle vehículos.");
        }
        return c;
    }

    /** Lista clientes activos que pueden ser propietarios.
     * @return clientes activos ordenados por nombre
     */
    @Transactional(readOnly = true)
    public List<Cliente> propietariosActivos() {
        return clientes.findByActivoTrueOrderByNombreNormalizadoAsc();
    }

    /** Registra un vehículo con un cliente y un taller activo específicos.
     * @param r datos normalizados y referencias elegidas
     * @return vehículo persistido
     * @throws NegocioException si cliente o taller no están disponibles o hay duplicado
     */
    @Transactional
    public Vehiculo registrar(VehiculoRequest r) {
        Taller ubicacion = taller.obtenerActivo(r.tallerId());
        Cliente cliente = clienteActivo(r.clienteId());
        DatosVehiculo d = normalizar(r);
        rechazarDuplicados(buscarCoincidencias(d, null));
        Vehiculo v = vehiculos.save(new Vehiculo(cliente, ubicacion, d));
        audit.record("VEHICULO", v.getId(), "VEHICULO_REGISTRADO",
                "placa=" + v.getPlaca() + " cliente=" + cliente.getId() + " taller=" + ubicacion.getId());
        historial.save(new HistorialVehiculo(v, v.getPlaca(), cliente.getNombreCompleto(), ubicacion.getNombre(),
                HistorialVehiculo.Tipo.REGISTRO,
                v.getMarca() + " " + v.getModelo() + " " + v.getAnio() + " registrado en " + ubicacion.getNombre()));
        return v;
    }

    /** Busca vehículos por texto e incluye, opcionalmente, los desactivados.
     * @param q filtro por placa, VIN, cliente o identificador; puede ser null
     * @param incluirInactivos indica si incluye vehículos desactivados
     * @return hasta el máximo configurado de vehículos
     */
    @Transactional(readOnly = true)
    public List<Vehiculo> consultar(String q, boolean incluirInactivos) {
        return consultar(q, null, null, null, incluirInactivos);
    }

    /** Busca vehículos por texto general o por propietario, placa y VIN.
     * @param q filtro general por texto o ID; puede ser null
     * @param clienteId ID exacto del propietario; puede ser null
     * @param placa fragmento de placa; puede ser null
     * @param vin fragmento de VIN; puede ser null
     * @param incluirInactivos indica si incluye vehículos desactivados
     * @return hasta cincuenta vehículos coincidentes
     */
    @Transactional(readOnly = true)
    public List<Vehiculo> consultar(String q, Long clienteId, String placa, String vin, boolean incluirInactivos) {
        String limpio = Texto.limpiar(q);
        PageRequest page = PageRequest.of(0, MAX_RESULTADOS);
        String alnum = limpio == null ? null : Texto.alfanumericoMayusculas(limpio);
        Long id = limpio != null && limpio.matches("^#?\\d{1,18}$") ? Long.parseLong(limpio.replace("#", "")) : null;
        String filtroPlaca = Texto.limpiar(placa);
        String filtroVin = Texto.limpiar(vin);
        return vehiculos.consultar(limpio == null ? null : "%" + Texto.normalizar(limpio) + "%",
                alnum == null ? null : "%" + alnum + "%", id, clienteId,
                filtroPlaca == null ? null : "%" + Texto.placa(filtroPlaca) + "%",
                filtroVin == null ? null : "%" + Texto.alfanumericoMayusculas(filtroVin) + "%",
                incluirInactivos, page);
    }

    /** Obtiene los vehículos cuyo cliente propietario coincide con el usuario autenticado.
     * @param email correo de la cuenta autenticada
     * @return vehículos del cliente con su taller asignado, si lo tienen
     */
    @Transactional(readOnly = true)
    public List<Vehiculo> misVehiculos(String email) {
        return vehiculos.findByClienteEmail(email);
    }

    /** Obtiene un vehículo por identificador.
     * @param id identificador consultado
     * @return vehículo existente
     * @throws NegocioException 404 cuando no existe
     */
    @Transactional(readOnly = true)
    public Vehiculo obtener(Long id) {
        Vehiculo v = vehiculos.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No existe un vehículo con ID " + id));
        v.getCliente().getNombreCompleto();
        return v;
    }

    /** Actualiza datos y ubicación, conservando un taller previo aunque esté inactivo.
     * @param id identificador del vehículo
     * @param r nuevos datos, cliente y taller
     * @return vehículo actualizado
     * @throws NegocioException si está inactivo, faltan referencias o hay duplicados
     */
    @Transactional
    public Vehiculo actualizar(Long id, VehiculoRequest r) {
        Vehiculo v = obtener(id);
        if (!v.isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El vehículo está desactivado. Reactívalo antes de corregir sus datos.");
        }
        DatosVehiculo d = normalizar(r);
        rechazarDuplicados(buscarCoincidencias(d, id));
        Long tallerAntesId = v.getTaller() == null ? null : v.getTaller().getId();
        String tallerAntes = v.getTaller() == null ? null : v.getTaller().getNombre();
        Long clienteAntesId = v.getCliente().getId();
        String clienteAntes = v.getCliente().getNombreCompleto();
        List<String> cambios = new ArrayList<>();
        comparar(cambios, "marca", v.getMarca(), d.marca());
        comparar(cambios, "modelo", v.getModelo(), d.modelo());
        comparar(cambios, "anio", String.valueOf(v.getAnio()), String.valueOf(d.anio()));
        comparar(cambios, "placa", v.getPlaca(), d.placa());
        comparar(cambios, "vin", v.getVin(), d.vin());
        comparar(cambios, "color", v.getColor(), d.color());
        comparar(cambios, "version", v.getVersion(), d.version());
        comparar(cambios, "kilometraje", v.getKilometraje() == null ? null : String.valueOf(v.getKilometraje()),
                d.kilometraje() == null ? null : String.valueOf(d.kilometraje()));
        comparar(cambios, "fotos", v.getFotos(), d.fotos());
        if (!Objects.equals(v.getCliente().getId(), r.clienteId())) {
            Cliente nuevo = clienteActivo(r.clienteId());
            cambios.add("cliente: " + v.getCliente().getId() + " -> " + nuevo.getId());
            v.cambiarCliente(nuevo);
        }
        Taller asignado = taller.obtener(r.tallerId());
        boolean conservaTaller = Objects.equals(v.getTaller() == null ? null : v.getTaller().getId(), asignado.getId());
        Taller ubicacion = conservaTaller ? asignado : taller.obtenerActivo(r.tallerId());
        if (!Objects.equals(v.getTaller() == null ? null : v.getTaller().getId(), ubicacion.getId())) {
            cambios.add("taller: " + (v.getTaller() == null ? "sin asignar" : v.getTaller().getId()) + " -> " + ubicacion.getId());
            v.cambiarTaller(ubicacion);
        }
        if (cambios.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "No hay cambios que guardar.");
        }
        String tallerAhora = ubicacion.getNombre();
        String clienteAhora = v.getCliente().getNombreCompleto();
        boolean cambioTaller = !Objects.equals(tallerAntesId, ubicacion.getId());
        boolean cambioCliente = !Objects.equals(clienteAntesId, v.getCliente().getId());
        v.aplicar(d);
        vehiculos.save(v);
        audit.record("VEHICULO", v.getId(), "VEHICULO_ACTUALIZADO", String.join("; ", cambios));
        if (cambioTaller) {
            historial.save(new HistorialVehiculo(v, v.getPlaca(), clienteAhora, tallerAhora,
                    HistorialVehiculo.Tipo.CAMBIO_TALLER,
                    "De " + (tallerAntes == null ? "sin asignar" : tallerAntes) + " a " + tallerAhora));
        }
        if (cambioCliente) {
            historial.save(new HistorialVehiculo(v, v.getPlaca(), clienteAhora, tallerAhora,
                    HistorialVehiculo.Tipo.CAMBIO_CLIENTE, "De " + clienteAntes + " a " + clienteAhora));
        }
        if (!cambioTaller && !cambioCliente) {
            historial.save(new HistorialVehiculo(v, v.getPlaca(), clienteAhora, tallerAhora,
                    HistorialVehiculo.Tipo.ACTUALIZACION, String.join("; ", cambios)));
        }
        return v;
    }

    /** Añade una diferencia textual si el valor cambió.
     * @param cambios lista de diferencias acumuladas
     * @param campo nombre visible del campo
     * @param antes valor persistido
     * @param despues nuevo valor
     * @return No aplica; actualiza la lista recibida.
     */
    private static void comparar(List<String> cambios, String campo, String antes, String despues) {
        if (!Objects.equals(antes, despues)) cambios.add(campo + ": '" + (antes == null ? "" : antes) + "' -> '" + (despues == null ? "" : despues) + "'");
    }

    /** Desactiva un vehículo sin borrar sus relaciones.
     * @param id identificador del vehículo
     * @param motivo motivo indicado por el operador
     * @return vehículo desactivado
     * @throws NegocioException cuando no existe o ya estaba desactivado
     */
    @Transactional
    public Vehiculo desactivar(Long id, String motivo) {
        Vehiculo v = obtener(id);
        if (!v.isActivo()) throw new NegocioException(HttpStatus.CONFLICT, "El vehículo ya está desactivado.");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        v.desactivar(Texto.limpiar(motivo), auth == null ? null : auth.getName());
        vehiculos.save(v);
        audit.record("VEHICULO", v.getId(), "VEHICULO_DESACTIVADO", "motivo=" + v.getMotivoDesactivacion());
        historial.save(new HistorialVehiculo(v, v.getPlaca(), v.getCliente().getNombreCompleto(),
                v.getTaller() == null ? null : v.getTaller().getNombre(),
                HistorialVehiculo.Tipo.DESACTIVACION, "motivo=" + v.getMotivoDesactivacion()));
        return v;
    }

    /** Reactiva un vehículo cuyo propietario sigue activo.
     * @param id identificador del vehículo
     * @return vehículo reactivado
     * @throws NegocioException cuando no existe, ya está activo o su cliente está inactivo
     */
    @Transactional
    public Vehiculo reactivar(Long id) {
        Vehiculo v = obtener(id);
        if (v.isActivo()) throw new NegocioException(HttpStatus.CONFLICT, "El vehículo ya está activo.");
        if (!v.getCliente().isActivo()) {
            throw new NegocioException(HttpStatus.CONFLICT, "El cliente propietario está desactivado. Reactívalo primero.");
        }
        v.reactivar();
        vehiculos.save(v);
        audit.record("VEHICULO", v.getId(), "VEHICULO_REACTIVADO", null);
        historial.save(new HistorialVehiculo(v, v.getPlaca(), v.getCliente().getNombreCompleto(),
                v.getTaller() == null ? null : v.getTaller().getNombre(),
                HistorialVehiculo.Tipo.REACTIVACION, null));
        return v;
    }

    /** Consulta movimientos por nombre de cliente o placa para el módulo de historial.
     * @param q texto de búsqueda; vacío devuelve los movimientos recientes
     * @return hasta cien movimientos del más reciente al más antiguo
     */
    @Transactional(readOnly = true)
    public List<HistorialVehiculo> historial(String q) {
        String limpio = Texto.limpiar(q);
        org.springframework.data.domain.PageRequest tope = org.springframework.data.domain.PageRequest.of(0, 100);
        return limpio == null ? historial.recientes(tope) : historial.buscarPorClienteOPlaca(limpio, tope);
    }

}

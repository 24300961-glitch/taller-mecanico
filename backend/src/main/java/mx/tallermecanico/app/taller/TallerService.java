package mx.tallermecanico.app.taller;

import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.error.NegocioException;
import mx.tallermecanico.app.sepomex.CodigoPostal;
import mx.tallermecanico.app.sepomex.CodigoPostalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reglas de alta, consulta y estado de talleres; no usa Strategy. */
@Service
public class TallerService {
    private static final Logger log = LoggerFactory.getLogger(TallerService.class);
    private final TallerRepository talleres;
    private final FotoTallerStorage fotos;
    private final AuditService audit;
    private final CodigoPostalRepository sepomex;

    /** Construye el servicio de talleres.
     * @param talleres repositorio de talleres
     * @param fotos almacenamiento de fotos
     * @param audit registro de eventos administrativos
     * @param sepomex catálogo local de códigos postales
     */
    public TallerService(TallerRepository talleres, FotoTallerStorage fotos, AuditService audit,
                         CodigoPostalRepository sepomex) {
        this.talleres = talleres;
        this.fotos = fotos;
        this.audit = audit;
        this.sepomex = sepomex;
    }

    /** Normaliza los textos de entrada a minúsculas salvo el RFC.
     * @param r formulario validado por Bean Validation
     * @return datos normalizados para persistencia
     */
    public DatosTaller normalizar(TallerRequest r) {
        if (Texto.soloDigitos(r.telefono()) == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Revisa los campos marcados: hay datos inválidos o incompletos.",
                    Map.of("errores", Map.of("telefono", "El teléfono debe incluir al menos un dígito.")));
        }
        return new DatosTaller(minusculas(r.nombre()), Texto.limpiar(r.rfc()).toUpperCase(),
                Texto.correo(r.email()), minusculas(r.telefono()), minusculas(r.calle()),
                minusculas(r.numero()), minusculas(r.colonia()), Texto.limpiar(r.codigoPostal()),
                minusculas(r.municipio()), minusculas(r.estado()), Boolean.TRUE.equals(r.activo()));
    }

    /** Inserta un taller si no existe otro con su RFC o nombre y domicilio completos.
     * @param request datos del formulario
     * @param foto foto opcional validada y almacenada fuera de MySQL
     * @return taller creado
     * @throws NegocioException 409 si hay duplicado y 400/500 por validación o almacenamiento
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Taller registrar(TallerRequest request, MultipartFile foto) {
        DatosTaller datos = normalizar(request);
        validarCpEstado(datos);
        rechazarDuplicado(datos, null);
        String fotoPath = fotos.guardar(foto);
        try {
            Taller taller = talleres.save(new Taller(datos, fotoPath));
            audit.record("TALLER", taller.getId(), "TALLER_REGISTRADO", "rfc=" + taller.getRfc());
            log.info("[Talleres] Taller {} registrado", taller.getId());
            return taller;
        } catch (RuntimeException ex) {
            fotos.eliminar(fotoPath);
            throw ex;
        }
    }

    /** Actualiza los datos y la foto de un taller existente.
     * @param id identificador del taller
     * @param request datos del formulario
     * @param foto foto nueva opcional que reemplaza la anterior
     * @return taller actualizado
     * @throws NegocioException 404 si no existe, 409 si duplica a otro y 400/500 por validación o almacenamiento
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Taller actualizar(Long id, TallerRequest request, MultipartFile foto) {
        Taller taller = obtener(id);
        DatosTaller datos = normalizar(request);
        validarCpEstado(datos);
        rechazarDuplicado(datos, id);
        String fotoAnterior = taller.getFotoPath();
        String fotoNueva = fotos.guardar(foto);
        try {
            taller.aplicar(datos);
            if (fotoNueva != null) taller.cambiarFoto(fotoNueva);
            Taller guardado = talleres.save(taller);
            if (fotoNueva != null) fotos.eliminar(fotoAnterior);
            audit.record("TALLER", id, "TALLER_ACTUALIZADO", "rfc=" + guardado.getRfc());
            log.info("[Talleres] Taller {} actualizado", id);
            return guardado;
        } catch (RuntimeException ex) {
            fotos.eliminar(fotoNueva);
            throw ex;
        }
    }

    /** Devuelve un taller por identificador.
     * @param id identificador solicitado
     * @return taller persistido
     * @throws NegocioException 404 si no existe
     */
    @Transactional(readOnly = true)
    public Taller obtener(Long id) {
        return talleres.findById(id).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                "No existe un taller con ID " + id));
    }

    /** Exige que el taller elegido exista y esté activo.
     * @param id identificador seleccionado
     * @return taller activo
     * @throws NegocioException 404 si falta y 409 si está inactivo
     */
    @Transactional(readOnly = true)
    public Taller obtenerActivo(Long id) {
        Taller taller = obtener(id);
        if (!taller.isActivo()) throw new NegocioException(HttpStatus.CONFLICT,
                "El taller \"" + taller.getNombre() + "\" está inactivo y no admite nuevas asociaciones.");
        if (!taller.isCompleto()) throw new NegocioException(HttpStatus.CONFLICT,
                "El taller \"" + taller.getNombre() + "\" tiene datos históricos incompletos y no admite nuevas asociaciones.");
        return taller;
    }

    /** Lista talleres, incluyendo inactivos solo cuando se solicita.
     * @param incluirInactivos indica si se incluyen talleres inactivos
     * @return talleres ordenados por nombre
     */
    @Transactional(readOnly = true)
    public List<Taller> consultar(boolean incluirInactivos) {
        return incluirInactivos ? talleres.findAll().stream().sorted(java.util.Comparator.comparing(Taller::getNombre)).toList()
                : talleres.findAllByActivoOrderByNombreAsc(true).stream().filter(Taller::isCompleto).toList();
    }

    /** Desactiva un taller sin eliminarlo ni alterar sus asociaciones.
     * @param id identificador de taller
     * @return taller inactivo
     * @throws NegocioException 404 si no existe
     */
    @Transactional
    public Taller desactivar(Long id) {
        Taller taller = obtener(id);
        taller.cambiarEstatus(false);
        Taller guardado = talleres.save(taller);
        audit.record("TALLER", id, "TALLER_DESACTIVADO", null);
        log.info("[Talleres] Taller {} desactivado", id);
        return guardado;
    }

    /** Reactiva un taller previamente inactivo.
     * @param id identificador de taller
     * @return taller activo
     * @throws NegocioException 404 si no existe
     */
    @Transactional
    public Taller reactivar(Long id) {
        Taller taller = obtener(id);
        taller.cambiarEstatus(true);
        Taller guardado = talleres.save(taller);
        audit.record("TALLER", id, "TALLER_REACTIVADO", null);
        return guardado;
    }

    /** Rechaza la eliminación física y comunica la alternativa de desactivación.
     * @param id identificador solicitado
     * @throws NegocioException siempre 409 después de confirmar que el taller existe
     */
    @Transactional(readOnly = true)
    public void eliminar(Long id) {
        Taller taller = obtener(id);
        throw new NegocioException(HttpStatus.CONFLICT, "El taller \"" + taller.getNombre()
                + "\" no se puede borrar. Cambia su estatus a inactivo.");
    }

    /** Comprueba los dos criterios solicitados de duplicidad.
     * @param d datos normalizados del taller
     * @param excluirId identificador propio al editar; null al registrar
     * @throws NegocioException 409 con los datos del taller existente
     */
    private void rechazarDuplicado(DatosTaller d, Long excluirId) {
        Taller existente = talleres.findFirstByRfc(d.rfc()).filter(t -> !t.getId().equals(excluirId)).orElse(null);
        if (existente == null) {
            existente = talleres.findByNombreAndCalleAndNumeroAndColoniaAndCodigoPostalAndMunicipioAndEstado(
                            d.nombre(), d.calle(), d.numero(), d.colonia(), d.codigoPostal(), d.municipio(), d.estado())
                    .stream().filter(t -> !t.getId().equals(excluirId)).findFirst().orElse(null);
        }
        if (existente != null) {
            log.warn("[Talleres] Alta duplicada bloqueada por coincidencia con taller {}", existente.getId());
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("codigo", "TALLER_DUPLICADO");
            detalle.put("tallerExistente", vistaBasica(existente));
            throw new NegocioException(HttpStatus.CONFLICT,
                    "No se creó el taller: ya existe uno con el mismo RFC o nombre y dirección.", detalle);
        }
    }

    /** Valida que el código postal pertenezca al estado indicado según el catálogo SEPOMEX.
     * Si el CP no existe en el catálogo local, los datos se aceptan para corrección manual.
     * @param d datos normalizados del taller
     * @throws NegocioException 400 cuando el catálogo conoce el CP pero el estado no coincide
     */
    private void validarCpEstado(DatosTaller d) {
        if (d.codigoPostal() == null || d.estado() == null) return;
        java.util.List<CodigoPostal> opciones = sepomex.findByCodigoPostalOrderByEstadoAscMunicipioAscColoniaAsc(d.codigoPostal());
        if (opciones.isEmpty()) return;
        String esperado = Texto.normalizar(d.estado());
        boolean coincide = opciones.stream().map(o -> Texto.normalizar(o.getEstado())).anyMatch(esperado::equals);
        if (!coincide) {
            String estados = opciones.stream().map(CodigoPostal::getEstado).distinct().sorted().toList().toString();
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El código postal " + d.codigoPostal() + " pertenece a " + estados + ", no al estado indicado.",
                    Map.of("errores", Map.of("estado", "El CP " + d.codigoPostal() + " no pertenece a este estado según SEPOMEX.",
                            "codigoPostal", "Revisa que el código postal corresponda al estado.")));
        }
    }

    /** Crea una representación segura del taller en conflicto.
     * @param taller registro existente
     * @return campos necesarios para que el front lo identifique
     */
    private static Map<String, Object> vistaBasica(Taller taller) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", taller.getId()); vista.put("nombre", taller.getNombre()); vista.put("rfc", taller.getRfc());
        vista.put("calle", taller.getCalle()); vista.put("numero", taller.getNumero());
        vista.put("colonia", taller.getColonia()); vista.put("codigoPostal", taller.getCodigoPostal());
        vista.put("municipio", taller.getMunicipio()); vista.put("estado", taller.getEstado());
        return vista;
    }

    /** Limpia y baja a minúsculas el texto ordinario.
     * @param value cadena recibida
     * @return cadena normalizada o null
     */
    private static String minusculas(String value) {
        String limpio = Texto.limpiar(value);
        return limpio == null ? null : limpio.toLowerCase(java.util.Locale.ROOT);
    }
}

package mx.tallermecanico.app.taller;

import mx.tallermecanico.app.error.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;

/** Valida y guarda fotos de taller como archivos fuera de MySQL. */
@Component
public class FotoTallerStorage {
    private static final Logger log = LoggerFactory.getLogger(FotoTallerStorage.class);
    private static final long MAX_BYTES = 15L * 1024 * 1024;
    private final Path storage;

    /** Inicializa la raíz persistente de imágenes.
     * @param storage ruta configurable de almacenamiento local
     */
    public FotoTallerStorage(@Value("${app.talleres.storage:storage/talleres}") String storage) {
        this.storage = preparar(Path.of(storage).toAbsolutePath().normalize());
    }

    /** Garantiza una carpeta escribible; si la configurada no lo es, usa una temporal.
     * @param base carpeta configurada para las fotos
     * @return carpeta lista para escribir
     */
    private static Path preparar(Path base) {
        try {
            Files.createDirectories(base);
            if (Files.isWritable(base)) return base;
            log.warn("[Talleres] La carpeta {} no es escribible; se usará una temporal.", base);
        } catch (IOException e) {
            log.warn("[Talleres] No se pudo preparar {}: {}. Se usará una temporal.", base, e.getMessage());
        }
        String nombre = base.getFileName() == null ? "fotos" : base.getFileName().toString();
        Path alterna = Path.of(System.getProperty("java.io.tmpdir"), "taller-mecanico", nombre)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(alterna);
            log.warn("[Talleres] Almacenamiento temporal activo: {}", alterna);
        } catch (IOException e) {
            log.error("[Talleres] Tampoco se pudo crear el almacenamiento temporal {}", alterna, e);
        }
        return alterna;
    }

    /** Valida extensión, peso y firma binaria, y persiste la foto con un nombre aleatorio.
     * @param file archivo enviado por el formulario; puede ser vacío
     * @return ruta relativa `talleres/<uuid>.<ext>` o null si no se adjuntó
     * @throws NegocioException si no es JPG/PNG, excede 15 MiB o no puede escribirse
     */
    public String guardar(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_BYTES) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La foto no debe exceder 15 MB.",
                    Map.of("errores", Map.of("foto", "La foto no debe exceder 15 MB.")));
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("webp")) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La foto solo acepta extensiones jpg, jpeg, png o webp.",
                    Map.of("errores", Map.of("foto", "La foto solo acepta extensiones jpg, jpeg, png o webp.")));
        }
        try {
            byte[] bytes = file.getBytes();
            boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            boolean png  = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                    && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
            // WebP: RIFF????WEBP (bytes 0-3 = "RIFF", bytes 8-11 = "WEBP")
            boolean webp = bytes.length >= 12
                    && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            boolean firmaValida = (ext.equals("png") && png)
                    || ((ext.equals("jpg") || ext.equals("jpeg")) && jpeg)
                    || (ext.equals("webp") && webp);
            if (!firmaValida) {
                throw new NegocioException(HttpStatus.BAD_REQUEST, "El contenido de la foto no coincide con su extensión (jpg, jpeg, png o webp).",
                        Map.of("errores", Map.of("foto", "El contenido no coincide con un archivo JPG, PNG o WebP válido.")));
            }
            Files.createDirectories(storage);
            String filename = UUID.randomUUID() + "." + ext;
            Path destination = storage.resolve(filename).normalize();
            if (!destination.startsWith(storage)) throw new NegocioException(HttpStatus.BAD_REQUEST, "La ruta de la foto no es válida.");
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return "talleres/" + filename;
        } catch (IOException ex) {
            log.error("[Talleres] No se pudo guardar la foto local en storage", ex);
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto del taller.",
                    Map.of("errores", Map.of("foto", "No se pudo guardar la foto en storage.")));
        }
    }

    /** Elimina una foto sustituida o una carga que no llegó a persistirse.
     * @param relativePath ruta relativa guardada en la tabla
     * @return No aplica; elimina el archivo si está bajo la carpeta configurada.
     */
    public void eliminar(String relativePath) {
        if (relativePath == null || !relativePath.startsWith("talleres/")) return;
        Path candidate = storage.resolve(relativePath.substring("talleres/".length())).normalize();
        if (!candidate.startsWith(storage)) return;
        try { Files.deleteIfExists(candidate); } catch (IOException ignored) { /* El dato persistido no se revierte por una limpieza fallida. */ }
    }

    /** Resuelve un archivo guardado para su descarga controlada.
     * @param relativePath ruta relativa `talleres/<uuid>.<ext>`
     * @return ruta absoluta del archivo o null si el nombre no es válido
     */
    public Path recurso(String relativePath) {
        if (relativePath == null || !relativePath.matches("^talleres/[A-Za-z0-9-]+\\.(?i)(jpg|jpeg|png|webp)$")) return null;
        Path candidate = storage.resolve(relativePath.substring("talleres/".length())).normalize();
        if (!candidate.startsWith(storage)) return null;
        return candidate;
    }

}

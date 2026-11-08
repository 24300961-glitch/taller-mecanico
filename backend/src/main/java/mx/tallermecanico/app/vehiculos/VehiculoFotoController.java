package mx.tallermecanico.app.vehiculos;

import mx.tallermecanico.app.error.NegocioException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Sube y sirve las fotos de los vehículos. */
@RestController
public class VehiculoFotoController {

    private final VehiculoFotoStorage fotos;

    /** Construye el controlador de fotos de vehículos.
     * @param fotos almacenamiento local de imágenes
     */
    public VehiculoFotoController(VehiculoFotoStorage fotos) {
        this.fotos = fotos;
    }

    /** Sube una foto y devuelve su ruta pública para guardarla en el vehículo.
     * @param foto imagen JPG, PNG o WebP de hasta 15 MB
     * @return ruta pública y mensaje de confirmación
     */
    @PostMapping(value = "/api/vehiculos/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('VEHICULOS_REGISTRAR','VEHICULOS_EDITAR')")
    public Map<String, Object> subir(@RequestParam("foto") MultipartFile foto) {
        return Map.of("ruta", fotos.guardar(foto), "message", "Foto agregada correctamente.");
    }

    /** Devuelve la imagen de un vehículo a usuarios autenticados.
     * @param archivo nombre del archivo `uuid.extensión`
     * @return bytes de la imagen con su tipo de contenido
     */
    @GetMapping("/storage/vehiculos/{archivo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> ver(@PathVariable String archivo) {
        Path ruta = fotos.recurso(archivo);
        if (ruta == null || !Files.isRegularFile(ruta)) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "La foto solicitada no existe.");
        }
        MediaType tipo = MediaTypeFactory.getMediaType(ruta.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }
}

package mx.tallermecanico.app.taller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/** Sirve las fotos de taller guardadas en disco a usuarios autenticados. */
@RestController
@RequestMapping("/storage/talleres")
public class FotoController {
    private final FotoTallerStorage fotos;

    /** Construye el controlador de fotos.
     * @param fotos almacenamiento local de imágenes
     */
    public FotoController(FotoTallerStorage fotos) {
        this.fotos = fotos;
    }

    /** Devuelve la imagen de un taller.
     * @param archivo nombre del archivo `uuid.extensión`
     * @return bytes de la imagen con su tipo de contenido
     */
    @GetMapping("/{archivo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> foto(@PathVariable String archivo) {
        Path ruta = fotos.recurso("talleres/" + archivo);
        if (ruta == null || !Files.isRegularFile(ruta)) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "La foto solicitada no existe.");
        }
        MediaType tipo = MediaTypeFactory.getMediaType(ruta.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }
}

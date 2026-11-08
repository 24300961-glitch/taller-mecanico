package mx.tallermecanico.app.sepomex;

import jakarta.persistence.EntityManager;
import mx.tallermecanico.app.common.Texto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Carga una vez el archivo oficial delimitado o CSV en la tabla postal local. */
@Component
public class SepomexCsvSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SepomexCsvSeeder.class);
    private static final int BATCH_SIZE = 1000;
    private final EntityManager entityManager;
    private final CodigoPostalRepository repository;
    private final String archivo;
    private final boolean importar;

    /** Configura el seeder sin asumir una ubicación de archivo ni cargarlo por defecto.
     * @param entityManager persistencia por lotes
     * @param repository conteo del catálogo
     * @param archivo ruta local configurada del archivo
     * @param importar habilitación explícita de carga inicial
     */
    public SepomexCsvSeeder(EntityManager entityManager, CodigoPostalRepository repository,
                            @Value("${app.sepomex.archivo:}") String archivo,
                            @Value("${app.sepomex.importar:false}") boolean importar) {
        this.entityManager = entityManager;
        this.repository = repository;
        this.archivo = archivo;
        this.importar = importar;
    }

    /** Importa el catálogo cuando se habilita y la tabla aún está vacía.
     * @param args argumentos de inicio de Spring
     * @return No aplica; persiste filas del catálogo.
     * @throws Exception si el archivo configurado no se puede leer o sus columnas requeridas faltan
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!importar || archivo == null || archivo.isBlank()) return;
        if (repository.count() > 0) {
            log.info("[SEPOMEX] Catálogo ya poblado; se omite la importación para evitar duplicados.");
            return;
        }
        Path path = Path.of(archivo).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            log.warn("[SEPOMEX] No se encontró el archivo local configurado: {}", path);
            return;
        }
        long inserted = importarArchivo(path);
        log.info("[SEPOMEX] Importación terminada: {} filas cargadas desde {}", inserted, path.getFileName());
    }

    /** Lee TXT oficial SEPOMEX delimitado por `|` o CSV delimitado por coma.
     * @param path archivo local con encabezado SEPOMEX o columnas oficiales en orden
     * @return número de filas persistidas
     * @throws Exception si falta una columna necesaria o falla la lectura
     */
    private long importarArchivo(Path path) throws Exception {
        List<String> lines = leerLineas(path);
        if (lines.isEmpty()) return 0;
        int inicio = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase(Locale.ROOT).contains("d_codigo")) { inicio = i; break; }
        }
        String first = lines.get(inicio);
        char delimiter = first.chars().filter(c -> c == '|').count() >= first.chars().filter(c -> c == ',').count() ? '|' : ',';
        String[] initial = split(first, delimiter);
        int[] columns = columnIndexes(initial);
        boolean hasHeader = columns != null;
        long rows = 0;
        int batch = 0;
        Set<String> seen = new HashSet<>();
        if (!hasHeader) { rows += insertRow(initial, columnsOfficial(), seen); batch++; }
        for (int i = inicio + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) continue;
            long added = insertRow(split(line, delimiter), columns == null ? columnsOfficial() : columns, seen);
            rows += added;
            batch += (int) added;
            if (batch >= BATCH_SIZE) {
                entityManager.flush(); entityManager.clear();
                batch = 0;
            }
        }
        entityManager.flush();
        return rows;
    }

    /** Lee todas las líneas detectando la codificación real del archivo descargado.
     * @param path archivo local del catálogo SEPOMEX
     * @return líneas del archivo sin finales de línea
     * @throws IOException si el archivo no se puede leer
     */
    private static List<String> leerLineas(Path path) throws java.io.IOException {
        // El archivo oficial CPdescarga.txt de SEPOMEX viene en ISO-8859-1; se lee
        // directamente en esa codificación para conservar acentos de colonias y municipios.
        byte[] bytes = Files.readAllBytes(path);
        String contenido = new String(bytes, java.nio.charset.Charset.forName("ISO-8859-1"));
        String limpio = contenido.replace("\uFEFF", "");
        return java.util.Arrays.asList(limpio.split("\r?\n"));
    }

    /** Persists one valid SEPOMEX row and filters exact duplicates in the file.
     * @param fields parsed values from one line
     * @param indexes source-column indexes for CP, colonia, municipio and estado
     * @param seen deduplication keys already loaded
     * @return 1 if inserted, otherwise 0
     */
    private long insertRow(String[] fields, int[] indexes, Set<String> seen) {
        int max = java.util.Arrays.stream(indexes).max().orElse(Integer.MAX_VALUE);
        if (fields.length <= max) return 0;
        String cp = clean(fields[indexes[0]]);
        String colonia = lower(fields[indexes[1]]);
        String municipio = lower(fields[indexes[2]]);
        String estado = lower(fields[indexes[3]]);
        if (cp == null || !cp.matches("\\d{5}") || colonia == null || municipio == null || estado == null) return 0;
        String key = cp + "|" + colonia + "|" + municipio + "|" + estado;
        if (!seen.add(key)) return 0;
        entityManager.persist(new CodigoPostal(cp, colonia, municipio, estado));
        return 1;
    }

    /** Identifica las cuatro columnas requeridas en una fila encabezado.
     * @param header nombres originales de columna
     * @return posiciones CP/colonia/municipio/estado o null si no es encabezado
     */
    private int[] columnIndexes(String[] header) {
        String[] names = java.util.Arrays.stream(header).map(SepomexCsvSeeder::headerName).toArray(String[]::new);
        int cp = find(names, "d_codigo", "codigo_postal", "codigo postal");
        int colonia = find(names, "d_asenta", "colonia", "asentamiento");
        int municipio = find(names, "d_mnpio", "d_munipio", "municipio");
        int estado = find(names, "d_estado", "estado");
        return cp >= 0 && colonia >= 0 && municipio >= 0 && estado >= 0 ? new int[]{cp, colonia, municipio, estado} : null;
    }

    /** Devuelve las posiciones oficiales del archivo SEPOMEX sin encabezado.
     * @return columnas cero-based d_codigo, d_asenta, D_mnpio y d_estado
     */
    private static int[] columnsOfficial() { return new int[]{0, 1, 3, 4}; }

    /** Busca un encabezado por cualquiera de sus nombres admitidos.
     * @param names nombres normalizados
     * @param candidates opciones de nombre
     * @return posición o -1
     */
    private static int find(String[] names, String... candidates) {
        for (int i = 0; i < names.length; i++) for (String candidate : candidates)
            if (names[i].equals(candidate)) return i;
        return -1;
    }

    /** Divide una fila CSV sencilla respetando delimitador del catálogo descargado.
     * @param line línea original
     * @param delimiter pipe oficial o coma CSV
     * @return columnas limpias de comillas externas y BOM
     */
    private static String[] split(String line, char delimiter) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        String cleanLine = line.replace("\uFEFF", "");
        for (int i = 0; i < cleanLine.length(); i++) {
            char c = cleanLine.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < cleanLine.length() && cleanLine.charAt(i + 1) == '"') { current.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == delimiter && !quoted) {
                parts.add(current.toString().trim()); current.setLength(0);
            } else current.append(c);
        }
        parts.add(current.toString().trim());
        return parts.toArray(String[]::new);
    }

    /** Normaliza etiquetas de encabezado para tolerar mayúsculas y acentos.
     * @param value etiqueta original
     * @return etiqueta normalizada
     */
    private static String headerName(String value) { return Texto.normalizar(value).replace(" ", "_"); }

    /** Limpia un campo sin cambiar códigos.
     * @param value valor de origen
     * @return valor normalizado o null
     */
    private static String clean(String value) { return Texto.limpiar(value); }

    /** Convierte datos de texto del catálogo a minúsculas.
     * @param value valor de origen
     * @return valor limpio en minúsculas o null
     */
    private static String lower(String value) {
        String result = Texto.limpiar(value);
        return result == null ? null : result.toLowerCase(Locale.ROOT);
    }
}

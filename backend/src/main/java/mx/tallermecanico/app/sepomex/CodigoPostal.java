package mx.tallermecanico.app.sepomex;

import jakarta.persistence.*;

/** Fila del catálogo local SEPOMEX usada para sugerencias por código postal. */
@Entity
@Table(name = "catalogo_codigos_postales", indexes = @Index(name = "idx_sep_codigo_postal", columnList = "codigo_postal"))
public class CodigoPostal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(nullable = false, length = 160)
    private String colonia;

    @Column(nullable = false, length = 120)
    private String municipio;

    @Column(nullable = false, length = 120)
    private String estado;

    protected CodigoPostal() { }

    /** Construye una fila del catálogo postal.
     * @param codigoPostal CP de cinco dígitos
     * @param colonia asentamiento publicado por SEPOMEX
     * @param municipio municipio publicado por SEPOMEX
     * @param estado entidad publicada por SEPOMEX
     * @return No aplica; inicializa la entidad.
     */
    public CodigoPostal(String codigoPostal, String colonia, String municipio, String estado) {
        this.codigoPostal = codigoPostal;
        this.colonia = colonia;
        this.municipio = municipio;
        this.estado = estado;
    }

    /** Devuelve el identificador persistido.
     * @return identificador o null antes de persistir
     */
    public Long getId() { return id; }
    /** Devuelve el código postal de la fila.
     * @return CP de cinco dígitos
     */
    public String getCodigoPostal() { return codigoPostal; }
    /** Devuelve el asentamiento de la fila.
     * @return nombre de colonia
     */
    public String getColonia() { return colonia; }
    /** Devuelve el municipio de la fila.
     * @return nombre de municipio
     */
    public String getMunicipio() { return municipio; }
    /** Devuelve la entidad de la fila.
     * @return nombre de estado
     */
    public String getEstado() { return estado; }
}

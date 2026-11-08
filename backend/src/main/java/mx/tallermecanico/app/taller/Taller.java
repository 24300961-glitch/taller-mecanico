package mx.tallermecanico.app.taller;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "talleres", indexes = @Index(name = "idx_talleres_nombre_cp", columnList = "nombre,codigo_postal"))
@EntityListeners(AuditingEntityListener.class)
public class Taller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 13, unique = true)
    private String rfc;

    @Column(length = 190)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "foto_path", length = 512)
    private String fotoPath;

    @Column(length = 160)
    private String calle;

    @Column(length = 40)
    private String numero;

    @Column(length = 120)
    private String colonia;

    @Column(name = "codigo_postal", length = 5)
    private String codigoPostal;

    @Column(length = 120)
    private String municipio;

    @Column(length = 120)
    private String estado;

    @Column(nullable = false)
    private boolean activo = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedBy
    @Column(length = 190)
    private String updatedBy;

    protected Taller() {}

    public Taller(String nombre, boolean activo) {
        this.nombre = nombre;
        this.activo = activo;
    }

    /** Crea un taller completo con los datos validados del formulario.
     * @param d datos normalizados del taller
     * @param fotoPath ruta relativa de la foto o null si no se adjuntó
     * @return No aplica; inicializa una entidad nueva.
     * @throws NullPointerException si los datos normalizados son null
     */
    public Taller(DatosTaller d, String fotoPath) {
        aplicar(d);
        this.fotoPath = fotoPath;
    }

    /** Actualiza los campos permitidos del taller.
     * @param d datos normalizados del formulario
     * @return No aplica; modifica esta entidad.
     * @throws NullPointerException si los datos normalizados son null
     */
    public void aplicar(DatosTaller d) {
        this.nombre = d.nombre();
        this.rfc = d.rfc();
        this.email = d.email();
        this.telefono = d.telefono();
        this.calle = d.calle();
        this.numero = d.numero();
        this.colonia = d.colonia();
        this.codigoPostal = d.codigoPostal();
        this.municipio = d.municipio();
        this.estado = d.estado();
        this.activo = d.activo();
    }

    /** Cambia el estatus activo del taller.
     * @param activo true para activo; false para inactivo
     * @return No aplica; modifica el estatus.
     */
    public void cambiarEstatus(boolean activo) { this.activo = activo; }

    /** Reemplaza la ruta relativa de la foto del taller.
     * @param fotoPath nueva ruta `talleres/...` o null si se retiró
     * @return No aplica; modifica esta entidad.
     */
    public void cambiarFoto(String fotoPath) { this.fotoPath = fotoPath; }

    /** Comprueba que el registro tenga todos los datos obligatorios de taller.
     * @return true cuando la fila puede ofrecerse para nuevas asociaciones
     */
    public boolean isCompleto() {
        return presente(nombre) && presente(rfc) && presente(email) && presente(telefono) && presente(calle)
                && presente(numero) && presente(colonia) && presente(codigoPostal) && presente(municipio) && presente(estado);
    }

    /** Comprueba que un campo heredado esté cargado.
     * @param valor cadena del campo
     * @return true si contiene texto
     */
    private static boolean presente(String valor) { return valor != null && !valor.isBlank(); }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRfc() { return rfc; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getFotoPath() { return fotoPath; }
    public String getCalle() { return calle; }
    public String getNumero() { return numero; }
    public String getColonia() { return colonia; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getMunicipio() { return municipio; }
    public String getEstado() { return estado; }
    public boolean isActivo() { return activo; }
}

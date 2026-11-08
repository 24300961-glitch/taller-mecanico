package mx.tallermecanico.app.clientes;

import jakarta.persistence.*;
import mx.tallermecanico.app.common.Texto;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import mx.tallermecanico.app.taller.Taller;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_clientes_nombre_norm", columnList = "nombreNormalizado"),
        @Index(name = "idx_clientes_tel", columnList = "telefonoNormalizado"),
        @Index(name = "idx_clientes_tel_alt", columnList = "telefonoAlternoNormalizado"),
        @Index(name = "idx_clientes_email", columnList = "email")
})
@EntityListeners(AuditingEntityListener.class)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(length = 80)
    private String apellidoPaterno;

    @Column(length = 80)
    private String apellidoMaterno;

    @Column(nullable = false, length = 210)
    private String nombreNormalizado;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 20)
    private String telefonoNormalizado;

    @Column(length = 20)
    private String telefonoAlterno;

    @Column(length = 20)
    private String telefonoAlternoNormalizado;

    @Column(length = 20)
    private String telefonoPersonal;

    @Column(length = 20)
    private String telefonoPersonalNormalizado;

    @Column(length = 20)
    private String telefonoTrabajo;

    @Column(length = 20)
    private String telefonoTrabajoNormalizado;

    @Column(length = 160)
    private String nombreContacto;

    @Column(length = 20)
    private String telefonoContacto;

    @Column(length = 20)
    private String telefonoContactoNormalizado;

    @Column(length = 190)
    private String email;

    @Column(length = 13)
    private String rfc;

    @Column(length = 250)
    private String direccion;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "taller_cliente",
            joinColumns = @JoinColumn(name = "cliente_id"),
            inverseJoinColumns = @JoinColumn(name = "taller_id"))
    private Set<Taller> talleres = new LinkedHashSet<>();

    @Column(length = 250)
    private String motivoDesactivacion;

    private Instant desactivadoEn;

    @Column(length = 190)
    private String desactivadoPor;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(length = 190, updatable = false)
    private String createdBy;

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedBy
    @Column(length = 190)
    private String updatedBy;

    protected Cliente() {}

    public Cliente(DatosCliente d) {
        aplicar(d);
    }

    public void aplicar(DatosCliente d) {
        this.nombre = d.nombre();
        this.apellidoPaterno = d.apellidoPaterno();
        this.apellidoMaterno = d.apellidoMaterno();
        String ap = ((d.apellidoPaterno() == null ? "" : d.apellidoPaterno()) + " "
                + (d.apellidoMaterno() == null ? "" : d.apellidoMaterno())).trim().replaceAll("\\s+", " ");
        this.apellidos = ap.isEmpty() ? null : ap;
        this.nombreNormalizado = Texto.normalizar(
                (d.nombre() == null ? "" : d.nombre()) + " " + ap);
        this.telefono = d.telefono();
        this.telefonoNormalizado = d.telefono();
        this.telefonoAlterno = d.telefonoAlterno();
        this.telefonoAlternoNormalizado = d.telefonoAlterno();
        this.telefonoPersonal = d.telefonoPersonal();
        this.telefonoPersonalNormalizado = d.telefonoPersonal();
        this.telefonoTrabajo = d.telefonoTrabajo();
        this.telefonoTrabajoNormalizado = d.telefonoTrabajo();
        this.nombreContacto = d.nombreContacto();
        this.telefonoContacto = d.telefonoContacto();
        this.telefonoContactoNormalizado = d.telefonoContacto();
        this.email = d.email();
        this.rfc = d.rfc();
        this.direccion = d.direccion();
        this.calle = d.calle();
        this.numero = d.numero();
        this.colonia = d.colonia();
        this.codigoPostal = d.codigoPostal();
        this.municipio = d.municipio();
        this.estado = d.estado();
    }

    public void desactivar(String motivo, String por) {
        this.activo = false;
        this.motivoDesactivacion = motivo;
        this.desactivadoEn = Instant.now();
        this.desactivadoPor = por;
    }

    public void reactivar() {
        this.activo = true;
        this.motivoDesactivacion = null;
        this.desactivadoEn = null;
        this.desactivadoPor = null;
    }

    /** Asocia talleres sin duplicar relaciones existentes.
     * @param talleres lista de talleres validados como activos
     * @return No aplica; actualiza la asociación de esta entidad.
     */
    public void asociarTalleres(java.util.Collection<Taller> talleres) {
        this.talleres.addAll(talleres);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellidos() { return apellidos; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getNombreCompleto() {
        return (nombre + " " + (apellidoPaterno == null ? "" : apellidoPaterno)
                + " " + (apellidoMaterno == null ? "" : apellidoMaterno)).trim().replaceAll("\\s+", " ");
    }
    public String getNombreNormalizado() { return nombreNormalizado; }
    public String getTelefono() { return telefono; }
    public String getTelefonoAlterno() { return telefonoAlterno; }
    public String getTelefonoPersonal() { return telefonoPersonal; }
    public String getTelefonoTrabajo() { return telefonoTrabajo; }
    public String getNombreContacto() { return nombreContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public String getEmail() { return email; }
    public String getRfc() { return rfc; }
    public String getDireccion() { return direccion; }
    public String getCalle() { return calle; }
    public String getNumero() { return numero; }
    public String getColonia() { return colonia; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getMunicipio() { return municipio; }
    public String getEstado() { return estado; }
    public boolean isActivo() { return activo; }
    /** Devuelve los talleres asociados.
     * @return conjunto de talleres; puede estar vacío en registros heredados
     */
    public Set<Taller> getTalleres() { return talleres; }
    public String getMotivoDesactivacion() { return motivoDesactivacion; }
    public Instant getDesactivadoEn() { return desactivadoEn; }
    public String getDesactivadoPor() { return desactivadoPor; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}

package mx.tallermecanico.app.seguridad;

import jakarta.persistence.*;
import mx.tallermecanico.app.common.Texto;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_roles_clave", columnNames = "clave"))
@EntityListeners(AuditingEntityListener.class)
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(nullable = false)
    private boolean asignable = true;

    @Column(nullable = false)
    private boolean activo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", joinColumns = @JoinColumn(name = "rol_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 40)
    private Set<Permiso> permisos = EnumSet.noneOf(Permiso.class);

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(length = 190, updatable = false)
    private String createdBy;

    protected Rol() {}

    public Rol(String nombre) {
        this.nombre = Texto.limpiar(nombre);
        this.clave = Texto.clave(nombre);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getClave() { return clave; }
    public boolean isAsignable() { return asignable; }
    public boolean isActivo() { return activo; }
    public Set<Permiso> getPermisos() { return permisos; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }

    public void setAsignable(boolean asignable) { this.asignable = asignable; }

    /** Suspende o reactiva el rol sin borrar su fila (soft-delete).
     * @param activo true para reactivar; false para suspender
     * @return No aplica; modifica esta entidad.
     */
    public void cambiarEstatus(boolean activo) { this.activo = activo; }
    public void otorgar(Set<Permiso> nuevos) { this.permisos.addAll(nuevos); }
    public boolean tiene(Permiso permiso) { return permisos.contains(permiso); }

    public void renombrar(String nombre) {
        this.nombre = Texto.limpiar(nombre);
        this.clave = Texto.clave(nombre);
    }
}

package mx.tallermecanico.app.vehiculos;

import jakarta.persistence.*;
import mx.tallermecanico.app.clientes.Cliente;
import mx.tallermecanico.app.taller.Taller;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "vehiculos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehiculos_placa", columnNames = "placa"),
        @UniqueConstraint(name = "uk_vehiculos_vin", columnNames = "vin")
}, indexes = @Index(name = "idx_vehiculos_taller", columnList = "taller_id"))
@EntityListeners(AuditingEntityListener.class)
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Permite conservar vehículos antiguos sin asignación durante la migración; los servicios nuevos exigen taller.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taller_id")
    private Taller taller;

    @Column(nullable = false, length = 40)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false, length = 15)
    private String placa;

    @Column(length = 17)
    private String vin;

    @Column(length = 30)
    private String color;

    @Column(length = 30)
    private String version;

    @Column
    private Integer kilometraje;

    @Column(name = "fotos", length = 500)
    private String fotos; // Almacena rutas de fotos separadas por coma

    @Column(nullable = false)
    private boolean activo = true;

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

    protected Vehiculo() {}

    public Vehiculo(Cliente cliente, DatosVehiculo d) {
        this.cliente = cliente;
        aplicar(d);
    }

    /** Crea un vehículo con propietario y taller asignado.
     * @param cliente propietario existente
     * @param taller taller activo seleccionado
     * @param d datos normalizados del vehículo
     * @return No aplica; inicializa la entidad nueva.
     */
    public Vehiculo(Cliente cliente, Taller taller, DatosVehiculo d) {
        this.cliente = cliente;
        this.taller = taller;
        aplicar(d);
    }

    /** Aplica los datos normalizados a la entidad.
     * @param d datos a aplicar
     */
    public void aplicar(DatosVehiculo d) {
        this.marca = d.marca();
        this.modelo = d.modelo();
        this.anio = d.anio();
        this.placa = d.placa();
        this.vin = d.vin();
        this.color = d.color();
        this.version = d.version();
        this.kilometraje = d.kilometraje();
        this.fotos = d.fotos();
    }

    public void cambiarCliente(Cliente cliente) { this.cliente = cliente; }

    /** Cambia la ubicación operativa del vehículo.
     * @param taller taller activo seleccionado
     * @return No aplica; actualiza la referencia.
     */
    public void cambiarTaller(Taller taller) { this.taller = taller; }

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

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Taller getTaller() { return taller; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getAnio() { return anio; }
    public String getPlaca() { return placa; }
    public String getVin() { return vin; }
    public String getColor() { return color; }
    public String getVersion() { return version; }
    public Integer getKilometraje() { return kilometraje; }
    public String getFotos() { return fotos; }
    public boolean isActivo() { return activo; }
    public String getMotivoDesactivacion() { return motivoDesactivacion; }
    public Instant getDesactivadoEn() { return desactivadoEn; }
    public String getDesactivadoPor() { return desactivadoPor; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}

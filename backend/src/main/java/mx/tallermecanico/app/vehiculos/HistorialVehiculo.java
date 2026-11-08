package mx.tallermecanico.app.vehiculos;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/** Movimiento histórico de un vehículo: registro, cambios de taller o cliente,
 * correcciones, desactivación y reactivación. Guarda los nombres como estaban
 * al momento del evento para que el historial no cambie si se renombran. */
@Entity
@Table(name = "historial_vehiculos", indexes = {
        @Index(name = "idx_historial_vehiculo", columnList = "vehiculo_id"),
        @Index(name = "idx_historial_cliente", columnList = "clienteNombre")
})
@EntityListeners(AuditingEntityListener.class)
public class HistorialVehiculo {

    /** Tipos de movimiento registrados. */
    public enum Tipo {
        REGISTRO, CAMBIO_TALLER, CAMBIO_CLIENTE, ACTUALIZACION, DESACTIVACION, REACTIVACION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false, length = 15)
    private String placa;

    @Column(nullable = false, length = 200)
    private String clienteNombre;

    @Column(length = 120)
    private String tallerNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(length = 500)
    private String detalle;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(length = 190, updatable = false)
    private String createdBy;

    protected HistorialVehiculo() {}

    /** Registra un movimiento con los nombres vigentes al momento del evento.
     * @param vehiculo vehículo involucrado
     * @param placa placa vigente
     * @param clienteNombre nombre del propietario al momento del evento
     * @param tallerNombre nombre del taller al momento del evento; puede ser null
     * @param tipo tipo de movimiento
     * @param detalle descripción corta del cambio; puede ser null
     */
    public HistorialVehiculo(Vehiculo vehiculo, String placa, String clienteNombre, String tallerNombre,
                             Tipo tipo, String detalle) {
        this.vehiculo = vehiculo;
        this.placa = placa;
        this.clienteNombre = clienteNombre;
        this.tallerNombre = tallerNombre;
        this.tipo = tipo;
        this.detalle = detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
    }

    public Long getId() { return id; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public String getPlaca() { return placa; }
    public String getClienteNombre() { return clienteNombre; }
    public String getTallerNombre() { return tallerNombre; }
    public Tipo getTipo() { return tipo; }
    public String getDetalle() { return detalle; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}

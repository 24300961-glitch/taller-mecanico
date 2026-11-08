package mx.tallermecanico.app.auth;

import jakarta.persistence.*;
import mx.tallermecanico.app.seguridad.Rol;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(nullable = false)
    private boolean enabled = true;

    private Instant lastLoginAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    @Column(length = 190, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 190)
    private String updatedBy;

    protected UserAccount() {}

    public UserAccount(String email, String fullName, String passwordHash, Rol rol) {
        this.email = email.toLowerCase().trim();
        this.fullName = fullName.trim();
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isEnabled() { return enabled; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    public void setFullName(String fullName) { this.fullName = fullName.trim(); }
    public void setEmail(String email) { this.email = email.toLowerCase().trim(); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void updatePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void asignarRol(Rol rol) { this.rol = rol; }
    public void touchLastLogin() { this.lastLoginAt = Instant.now(); }
}
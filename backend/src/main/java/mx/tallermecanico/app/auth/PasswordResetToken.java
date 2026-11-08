package mx.tallermecanico.app.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=64) private String tokenHash;
    @ManyToOne(optional=false) private UserAccount user;
    @Column(nullable=false) private Instant expiresAt;
    private Instant usedAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(String tokenHash, UserAccount user, Instant expiresAt) { this.tokenHash=tokenHash; this.user=user; this.expiresAt=expiresAt; }
    public UserAccount getUser(){return user;} public Instant getExpiresAt(){return expiresAt;} public Instant getUsedAt(){return usedAt;} public void markUsed(){usedAt=Instant.now();}
}

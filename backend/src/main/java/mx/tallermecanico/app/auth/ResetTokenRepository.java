package mx.tallermecanico.app.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String hash);
    List<PasswordResetToken> findByUserAndUsedAtIsNull(UserAccount user);
}

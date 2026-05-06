package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.PasswordResetToken;
import org.djezzy.pfe.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenDAO extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUser(User user);

    Optional<PasswordResetToken> findByUserAndToken(User user, String token);
}

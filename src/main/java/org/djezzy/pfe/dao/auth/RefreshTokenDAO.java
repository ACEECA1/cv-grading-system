package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.RefreshToken;
import org.djezzy.pfe.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenDAO extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}



package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.model.auth.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeDAO extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<VerificationCode> findByUserAndCodeAndUsedFalse(User user, String code);
}



package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.User;
import org.djezzy.pfe.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeDAO extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<VerificationCode> findByUserAndCodeAndUsedFalse(User user, String code);
}

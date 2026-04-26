package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.StructuredJd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StructuredJdDAO extends JpaRepository<StructuredJd, Long> {
    Optional<StructuredJd> findByJobOfferId(Long jobOfferId);
}

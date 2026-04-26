package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.HRPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HRPersonDAO extends JpaRepository<HRPerson, Long> {
}

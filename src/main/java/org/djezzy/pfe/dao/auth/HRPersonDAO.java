package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.HRPerson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HRPersonDAO extends JpaRepository<HRPerson, Long> {
}



package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDAO extends JpaRepository<Admin, Long> {
}



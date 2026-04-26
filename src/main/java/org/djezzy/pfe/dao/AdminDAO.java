package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminDAO extends JpaRepository<Admin, Long> {
}

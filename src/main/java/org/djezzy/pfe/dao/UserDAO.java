package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.RhApprovalStatus;
import org.djezzy.pfe.model.Role;
import org.djezzy.pfe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDAO extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRoleAndRhApprovalStatus(Role role, RhApprovalStatus hrApprovalStatus);
}

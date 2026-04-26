package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.ProfileData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileDataDAO extends JpaRepository<ProfileData, Long> {
}

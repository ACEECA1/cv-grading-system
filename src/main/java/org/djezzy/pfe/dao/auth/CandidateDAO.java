package org.djezzy.pfe.dao.auth;

import org.djezzy.pfe.model.auth.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateDAO extends JpaRepository<Candidate, Long> {
}



package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateDAO extends JpaRepository<Candidate, Long> {
}

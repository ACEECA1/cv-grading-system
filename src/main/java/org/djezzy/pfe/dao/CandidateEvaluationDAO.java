package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.CandidateEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateEvaluationDAO extends JpaRepository<CandidateEvaluation, Long> {
    Optional<CandidateEvaluation> findByCvId(Long cvId);
}

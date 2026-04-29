package org.djezzy.pfe.dao.evaluation;

import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CandidateEvaluationDAO extends JpaRepository<CandidateEvaluation, Long>, JpaSpecificationExecutor<CandidateEvaluation> {
    Optional<CandidateEvaluation> findByCvId(Long cvId);

    @Query("select avg(ms.overallScore) from CandidateEvaluation ce join ce.matchScore ms where ms.overallScore is not null")
    Double findAverageMatchScore();
}



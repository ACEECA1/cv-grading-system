package org.djezzy.pfe.dao.evaluation;

import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CandidateEvaluationDAO extends JpaRepository<CandidateEvaluation, Long> {
    Optional<CandidateEvaluation> findByCvId(Long cvId);
    List<CandidateEvaluation> findByCv_JobOffer_Id(Long jobOfferId, Sort sort);

    @Query("select avg(ms.overallScore) from CandidateEvaluation ce join ce.matchScore ms where ms.overallScore is not null")
    Double findAverageMatchScore();
}

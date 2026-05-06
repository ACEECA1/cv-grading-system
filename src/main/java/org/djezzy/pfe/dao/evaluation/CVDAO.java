package org.djezzy.pfe.dao.evaluation;

import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CVDAO extends JpaRepository<CV, Long> {
    List<CV> findByCandidateIdOrderByUploadDateDesc(Long candidateId);
    List<CV> findByJobOfferIdOrderByUploadDateDesc(Long jobOfferId);

    Optional<CV> findTopByCandidateIdAndJobOfferIdOrderByUploadDateDesc(Long candidateId, Long jobOfferId);

    long countByStatusIn(Collection<CVProcessingStatus> statuses);
}



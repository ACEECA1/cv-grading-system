package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.CV;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CVDAO extends JpaRepository<CV, Long> {
    List<CV> findByCandidateIdOrderByUploadDateDesc(Long candidateId);
}

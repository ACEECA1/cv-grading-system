package org.djezzy.pfe.dao;

import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.model.JobOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobOfferDAO extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findByStatus(JobOfferStatus status);
}

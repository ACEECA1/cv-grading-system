package org.djezzy.pfe.dao.job;

import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobOfferDAO extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    List<JobOffer> findByStatus(JobOfferStatus status);
}



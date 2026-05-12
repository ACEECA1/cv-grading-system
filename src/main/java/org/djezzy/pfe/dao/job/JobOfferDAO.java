package org.djezzy.pfe.dao.job;

import jakarta.persistence.criteria.JoinType;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobOfferDAO extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {
    List<JobOffer> findByStatus(JobOfferStatus status);

    default Page<JobOffer> findAllByFilters(String title, String location, Boolean isPublished, Pageable pageable) {
        Specification<JobOffer> specification = Specification.where(null);

        if (title != null && !title.isBlank()) {
            String normalizedTitle = "%" + title.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), normalizedTitle));
        }

        if (location != null && !location.isBlank()) {
            String normalizedLocation = "%" + location.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.join("structuredJd", JoinType.LEFT).get("workLocation")),
                            normalizedLocation
                    ));
        }

        if (isPublished != null) {
            specification = specification.and((root, query, cb) ->
                    isPublished
                            ? cb.equal(root.get("status"), JobOfferStatus.PUBLISHED)
                            : cb.notEqual(root.get("status"), JobOfferStatus.PUBLISHED));
        }

        return findAll(specification, pageable);
    }
}



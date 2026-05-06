package org.djezzy.pfe.dto.job;

import org.djezzy.pfe.model.job.JobOfferStatus;

import java.time.Instant;

public record JobOfferDetailDTO(
        Long id,
        String title,
        JobOfferStatus status,
        Instant createdAt,
        StructuredJdDTO structuredJd
) {
}

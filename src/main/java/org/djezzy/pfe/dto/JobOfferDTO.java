package org.djezzy.pfe.dto;

import org.djezzy.pfe.model.JobOfferStatus;

import java.time.Instant;

public record JobOfferDTO(
        Long id,
        String title,
        String rawText,
        JobOfferStatus status,
        String jdRequestId,
        StructuredJdDTO structuredJd,
        Instant createdAt,
        Instant updatedAt
) {
}

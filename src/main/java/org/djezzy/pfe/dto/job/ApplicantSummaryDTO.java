package org.djezzy.pfe.dto.job;

import java.time.Instant;

public record ApplicantSummaryDTO(
        Long evaluationId,
        String candidateName,
        Double matchScore,
        String status,
        Instant applicationDate
) {
}

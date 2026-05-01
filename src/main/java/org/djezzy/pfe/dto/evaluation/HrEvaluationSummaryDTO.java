package org.djezzy.pfe.dto.evaluation;


import org.djezzy.pfe.model.evaluation.EvaluationStatus;

import java.time.Instant;

public record HrEvaluationSummaryDTO(
        Long evaluationId,
        EvaluationStatus status,
        Double overallScore,
        Long cvId,
        Instant cvUploadDate,
        Long candidateId,
        String candidateFullName,
        Long jobOfferId,
        String jobOfferTitle
) {
}





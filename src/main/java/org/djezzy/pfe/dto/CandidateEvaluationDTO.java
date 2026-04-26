package org.djezzy.pfe.dto;

import org.djezzy.pfe.model.EvaluationStatus;

public record CandidateEvaluationDTO(
        Long id,
        EvaluationStatus status,
        Double overallScore,
        String detailsJson
) {
}

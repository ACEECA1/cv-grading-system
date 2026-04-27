package org.djezzy.pfe.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import org.djezzy.pfe.model.EvaluationStatus;

public record EvaluationCallbackRequest(
        @NotNull EvaluationStatus status,
        Double overallScore,
        String detailsJson,
        @JsonAlias("evaluationId") Long candidateEvaluationId
) {
}

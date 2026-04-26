package org.djezzy.pfe.dto;

import jakarta.validation.constraints.NotNull;
import org.djezzy.pfe.model.EvaluationStatus;

public record EvaluationCallbackRequest(
        @NotNull EvaluationStatus status,
        Double overallScore,
        String detailsJson
) {
}

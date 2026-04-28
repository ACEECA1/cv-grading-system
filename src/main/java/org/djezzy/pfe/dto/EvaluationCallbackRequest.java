package org.djezzy.pfe.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import org.djezzy.pfe.model.EvaluationStatus;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluationCallbackRequest(
        @NotNull EvaluationStatus status,
        Double overallScore,
        String detailsJson,
        @JsonAlias("evaluationId") Long candidateEvaluationId,
        @JsonAlias("match_score") MatchScorePayload matchScore
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchScorePayload(
            @JsonAlias("overall_score") Double overallScore,
            @JsonAlias("matched_skills") List<String> matchedSkills,
            @JsonAlias("missing_skills") List<String> missingSkills,
            String recommendation,
            String reasoning
    ) {
    }
}

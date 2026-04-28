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
        @JsonAlias("match_score") MatchScoreDTO matchScore,
        @JsonAlias("profile_data") ProfileDataDTO profileData,
        @JsonAlias("structured_jd") StructuredJdDTO structuredJd,
        @JsonAlias("technical_questions") List<TechnicalQuestionDTO> technicalQuestions,
        @JsonAlias("hr_questions") List<HrQuestionDTO> hrQuestions
) {
}

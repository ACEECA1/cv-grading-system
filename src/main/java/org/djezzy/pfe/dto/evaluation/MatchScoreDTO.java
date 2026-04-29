package org.djezzy.pfe.dto.evaluation;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchScoreDTO(
        @JsonAlias("overall_score")
        Double overallScore,
        @JsonAlias("matched_skills")
        List<String> matchedSkills,
        @JsonAlias("missing_skills")
        List<String> missingSkills,
        @JsonAlias("experience_alignment")
        ExperienceAlignmentPayload experienceAlignment,
        @JsonAlias("education_match")
        EducationMatchPayload educationMatch,
        String recommendation,
        String reasoning
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperienceAlignmentPayload(
            @JsonAlias("years_required") String yearsRequired,
            @JsonAlias("years_candidate") String yearsCandidate,
            @JsonAlias("match_percentage") String matchPercentage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationMatchPayload(
            @JsonAlias("required_degree") String requiredDegree,
            @JsonAlias("candidate_degree") String candidateDegree,
            @JsonAlias("match_status") String matchStatus
    ) {
    }
}





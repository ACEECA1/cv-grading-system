package org.djezzy.pfe.dto.evaluation;


import org.djezzy.pfe.model.evaluation.EvaluationStatus;

import java.util.List;

public record CandidateEvaluationDTO(
        Long id,
        EvaluationStatus status,
        Double overallScore,
        String recommendation,
        String reasoning,
        List<String> matchedSkills,
        List<MissingSkillDTO> missingSkills,
        ExperienceAlignmentDTO experienceAlignment,
        EducationMatchDTO educationMatch
) {
    public record MissingSkillDTO(
            String skillName,
            String importance
    ) {
    }

    public record YearsRequiredDTO(
            Integer min,
            Integer max
    ) {
    }

    public record ExperienceAlignmentDTO(
            YearsRequiredDTO yearsRequired,
            Integer yearsCandidate,
            Double matchScore
    ) {
    }

    public record EducationMatchDTO(
            String requiredDegree,
            String candidateDegree,
            org.djezzy.pfe.model.evaluation.MatchLevel matchLevel,
            String reasoning
    ) {
    }
}




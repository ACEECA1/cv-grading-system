package org.djezzy.pfe.dto.evaluation;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import org.djezzy.pfe.model.evaluation.EvaluationStatus;

import java.util.List;

public record CandidateEvaluationDTO(
        Long id,
        EvaluationStatus status,
        Double overallScore,
        String detailsJson,
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

    public record ExperienceAlignmentDTO(
            Double yearsRequired,
            Double yearsCandidate,
            Double matchPercentage
    ) {
    }

    public record EducationMatchDTO(
            String requiredDegree,
            String candidateDegree,
            String matchStatus
    ) {
    }
}





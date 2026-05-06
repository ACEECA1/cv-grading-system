package org.djezzy.pfe.dto.evaluation;

import org.djezzy.pfe.model.evaluation.EvaluationStatus;

import java.time.Instant;
import java.util.List;

public record HrEvaluationDetailDTO(
        Long id,
        EvaluationStatus status,
        Double overallScore,
        String recommendation,
        String reasoning,
        List<String> matchedSkills,
        List<CandidateEvaluationDTO.MissingSkillDTO> missingSkills,
        CandidateEvaluationDTO.ExperienceAlignmentDTO experienceAlignment,
        CandidateEvaluationDTO.EducationMatchDTO educationMatch,
        Long cvId,
        Instant cvUploadDate,
        Long candidateId,
        String candidateFullName,
        Long jobOfferId,
        String jobOfferTitle,
        ProfileDataDTO profileData,
        List<TechnicalQuestionDTO> technicalQuestions,
        List<HrQuestionDTO> hrQuestions
) {
    public record ProfileDataDTO(
            PersonalInfoDTO personalInfo,
            List<ExperienceItemDTO> experience,
            List<EducationItemDTO> education,
            List<String> languages,
            List<String> skills
    ) {
    }

    public record PersonalInfoDTO(
            String email,
            String phone,
            String location,
            String linkedin
    ) {
    }

    public record ExperienceItemDTO(
            String title,
            String company,
            String duration,
            String description
    ) {
    }

    public record EducationItemDTO(
            String degree,
            String institution,
            String year
    ) {
    }
}


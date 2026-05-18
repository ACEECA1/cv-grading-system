package org.djezzy.pfe.dto.evaluation;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record N8nEvaluationPayloadDTO(
        @NotBlank String status,
        @JsonProperty("processing_time") String processingTime,
        @JsonAlias("profileData")
        @JsonProperty("profile_data") ProfileDataDTO profileData,
        @NotNull
        @JsonProperty("match_score") MatchScoreDTO matchScore,
        @JsonAlias("technical_assessment")
        @JsonProperty("technical_questions") List<TechnicalQuestionDTO> technicalQuestions,
        @JsonAlias("hr_assessment")
        @JsonProperty("hr_questions") List<HrQuestionDTO> hrQuestions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileDataDTO(
            @JsonAlias("personalInfo")
            @JsonProperty("personal_info") PersonalInfoDTO personalInfo,
            @JsonAlias("experience")
            List<ExperienceDTO> experiences,
            List<EducationDTO> education,
            List<String> skills,
            List<LanguageDTO> languages,
            List<CertificateDTO> certificates,
            List<String> hobbies,
            @JsonAlias("contactInfo")
            @JsonProperty("contact_info") ContactInfoDTO contactInfo,
            @JsonAlias("normalizedSkills")
            @JsonProperty("normalized_skills") List<NormalizedSkillDTO> normalizedSkills
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonalInfoDTO(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String email,
            String phone,
            String location
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperienceDTO(
            @JsonProperty("title") String title,
            @JsonProperty("company") String company,
            @JsonProperty("duration") @JsonAlias({"start_date", "end_date"}) String duration,
            @JsonProperty("description") String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationDTO(
            String degree,
            String institution,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            String honors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageDTO(
            String language,
            String level
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CertificateDTO(
            String name,
            String issuer,
            String date
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContactInfoDTO(
            String email,
            String phone,
            String linkedin
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NormalizedSkillDTO(
            @JsonProperty("original_name") String originalName,
            @JsonProperty("normalized_name") String normalizedName,
            String category,
            @JsonProperty("proficiency_level") String proficiencyLevel,
            @JsonProperty("years_experience") Double yearsExperience
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchScoreDTO(
            @JsonAlias("overall_match_score")
            @JsonProperty("overall_score") Double overallScore,
            @JsonProperty("matched_skills") List<String> matchedSkills,
            @JsonProperty("missing_skills") List<MissingSkillDTO> missingSkills,
            @JsonProperty("experience_alignment") ExperienceAlignmentDTO experienceAlignment,
            @JsonProperty("education_match") EducationMatchDTO educationMatch,
            String recommendation,
            String reasoning
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MissingSkillDTO(
            @JsonProperty("skill_name") String skillName,
            String importance
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperienceAlignmentDTO(
            @JsonProperty("years_required") Integer yearsRequired,
            @JsonProperty("years_candidate") Integer yearsCandidate,
            @JsonProperty("match_score")
            Double matchScore
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationMatchDTO(
            @JsonProperty("required_degree") String requiredDegree,
            @JsonProperty("candidate_degree") String candidateDegree,
            @JsonProperty("match_status") String matchStatus
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TechnicalQuestionDTO(
            @JsonProperty("question") String question,
            @JsonProperty("expected_answer") @JsonAlias("expectedAnswer") String expectedAnswer,
            @JsonProperty("difficulty") String difficulty,
            @JsonProperty("skill_area") String skillArea,
            @JsonProperty("bluff_indicator") boolean bluffIndicator,
            @JsonProperty("follow_up_questions") List<String> followUpQuestions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HrQuestionDTO(
            @JsonProperty("question") String question,
            @JsonProperty("psychological_intent") @JsonAlias("purpose") String psychologicalIntent,
            @JsonProperty("evaluation_criteria") String evaluationCriteria,
            @JsonProperty("ideal_response_indicators") List<String> idealResponseIndicators,
            @JsonProperty("red_flags") List<String> redFlags,
            @JsonProperty("follow_up_probes") List<String> followUpProbes
    ) {
    }
}

package org.djezzy.pfe.util;

import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.evaluation.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.job.ExperienceRangeDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.StructuredJdDTO;
import org.djezzy.pfe.dto.auth.UserDTO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.evaluation.MatchScore;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.model.auth.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MapperUtil {
    public UserDTO toUserDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getIsEnabled(),
                user.getRhApprovalStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public StructuredJdDTO toStructuredJdDto(StructuredJd structuredJd) {
        if (structuredJd == null) {
            return null;
        }
        ExperienceRangeDTO rangeDTO = structuredJd.getExperienceRange() == null
                ? null
                : new ExperienceRangeDTO(structuredJd.getExperienceRange().getMinYears(), structuredJd.getExperienceRange().getMaxYears());

        return new StructuredJdDTO(
                structuredJd.getId(),
                structuredJd.getTitle(),
                structuredJd.getCompanyName(),
                structuredJd.getRequiredSkills().stream().map(s -> s.getName()).toList(),
                structuredJd.getPreferredSkills().stream().map(s -> s.getName()).toList(),
                rangeDTO,
                structuredJd.getResponsibilities().stream().map(r -> r.getDescription()).toList(),
                structuredJd.getQualifications().stream().map(qualification -> qualification.getDescription()).toList(),
                structuredJd.getWorkLocation()
        );
    }

    public JobOfferDTO toJobOfferDto(JobOffer jobOffer) {
        return new JobOfferDTO(
                jobOffer.getId(),
                jobOffer.getTitle(),
                jobOffer.getRawText(),
                jobOffer.getStatus(),
                jobOffer.getJdRequestId(),
                toStructuredJdDto(jobOffer.getStructuredJd()),
                jobOffer.getCreatedAt(),
                jobOffer.getUpdatedAt()
        );
    }

    public CandidateEvaluationDTO toCandidateEvaluationDto(CandidateEvaluation evaluation) {
        MatchScore matchScore = evaluation.getMatchScore();
        Double overallScore = matchScore == null ? null : matchScore.getOverallScore();
        String recommendation = matchScore == null ? null : matchScore.getRecommendation();
        String reasoning = matchScore == null ? null : matchScore.getReasoning();
        List<String> matchedSkills = matchScore == null
                ? List.of()
                : matchScore.getMatchedSkills().stream().map(item -> item.getName()).toList();
        List<CandidateEvaluationDTO.MissingSkillDTO> missingSkills = matchScore == null
                ? List.of()
                : matchScore.getMissingSkills().stream()
                .map(item -> new CandidateEvaluationDTO.MissingSkillDTO(item.getSkillName(), item.getImportance()))
                .toList();
        CandidateEvaluationDTO.ExperienceAlignmentDTO experienceAlignment = matchScore == null || matchScore.getExperienceAlignment() == null
                ? null
                : new CandidateEvaluationDTO.ExperienceAlignmentDTO(
                matchScore.getExperienceAlignment().getYearsRequired(),
                matchScore.getExperienceAlignment().getYearsCandidate(),
                matchScore.getExperienceAlignment().getMatchPercentage()
        );
        CandidateEvaluationDTO.EducationMatchDTO educationMatch = matchScore == null || matchScore.getEducationMatch() == null
                ? null
                : new CandidateEvaluationDTO.EducationMatchDTO(
                matchScore.getEducationMatch().getRequiredDegree(),
                matchScore.getEducationMatch().getCandidateDegree(),
                matchScore.getEducationMatch().getMatchStatus()
        );
        return new CandidateEvaluationDTO(
                evaluation.getId(),
                evaluation.getStatus(),
                overallScore,
                evaluation.getDetailsJson(),
                recommendation,
                reasoning,
                matchedSkills,
                missingSkills,
                experienceAlignment,
                educationMatch
        );
    }

    public CandidateSubmissionDTO toCandidateSubmissionDto(CV cv) {
        CandidateEvaluationDTO evaluationDTO = cv.getCandidateEvaluation() == null ? null : toCandidateEvaluationDto(cv.getCandidateEvaluation());
        return new CandidateSubmissionDTO(
                cv.getId(),
                cv.getFileUrl(),
                cv.getRawText(),
                cv.getStatus(),
                cv.getUploadDate(),
                toJobOfferDto(cv.getJobOffer()),
                evaluationDTO
        );
    }

    public List<JobOfferDTO> toJobOfferDtos(List<JobOffer> jobOffers) {
        return jobOffers.stream().map(this::toJobOfferDto).toList();
    }

    public List<CandidateSubmissionDTO> toCandidateSubmissionDtos(List<CV> cvs) {
        return cvs.stream().map(this::toCandidateSubmissionDto).toList();
    }
}


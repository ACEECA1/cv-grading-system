package org.djezzy.pfe.util;

import org.djezzy.pfe.dto.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.ExperienceRangeDTO;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.StructuredJdDTO;
import org.djezzy.pfe.dto.UserDTO;
import org.djezzy.pfe.model.CV;
import org.djezzy.pfe.model.CandidateEvaluation;
import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.model.StructuredJd;
import org.djezzy.pfe.model.User;
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
        Double overallScore = null;
        if (evaluation.getMatchScore() != null) {
            overallScore = evaluation.getMatchScore().getOverallScore();
        }
        return new CandidateEvaluationDTO(
                evaluation.getId(),
                evaluation.getStatus(),
                overallScore,
                evaluation.getDetailsJson()
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

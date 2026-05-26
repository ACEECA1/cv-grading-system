package org.djezzy.pfe.service.evaluation;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.evaluation.HrQuestionDTO;
import org.djezzy.pfe.dto.evaluation.HrEvaluationDetailDTO;
import org.djezzy.pfe.dto.system.DashboardStatsDTO;
import org.djezzy.pfe.dto.evaluation.TechnicalQuestionDTO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.MatchScore;
import org.djezzy.pfe.model.evaluation.ProfileData;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.FileStorageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrService {
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final CVDAO cvdao;
    private final UserDAO userDAO;
    private final FileStorageUtil fileStorageUtil;

    @Transactional(readOnly = true)
    public DashboardStatsDTO dashboardStats() {
        long totalCvsProcessed = cvdao.count();
        Double average = candidateEvaluationDAO.findAverageMatchScore();
        long pendingHrApprovals = userDAO.countByRoleAndRhApprovalStatus(Role.HR, RhApprovalStatus.PENDING);
        return new DashboardStatsDTO(
                totalCvsProcessed,
                average == null ? 0.0 : average,
                pendingHrApprovals
        );
    }

    @Transactional(readOnly = true)
    public HrEvaluationDetailDTO getEvaluation(Long evaluationId) {
        CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        return toDetailDto(evaluation);
    }

    @Transactional(readOnly = true)
    public Path resolveEvaluationCvPath(Long evaluationId) {
        CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        CV cv = evaluation.getCv();
        if (cv == null || cv.getFileUrl() == null || cv.getFileUrl().isBlank()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Stored CV file path is missing");
        }
        if (!fileStorageUtil.exists(cv.getFileUrl())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Stored CV file not found");
        }
        return fileStorageUtil.resolve(cv.getFileUrl());
    }

    private HrEvaluationDetailDTO toDetailDto(CandidateEvaluation evaluation) {
        CV cv = evaluation.getCv();
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
                toIntegerYears(matchScore.getExperienceAlignment().getYearsRequired()),
                toIntegerYears(matchScore.getExperienceAlignment().getYearsCandidate()),
                matchScore.getExperienceAlignment().getMatchPercentage()
        );
        CandidateEvaluationDTO.EducationMatchDTO educationMatch = matchScore == null || matchScore.getEducationMatch() == null
                ? null
                : new CandidateEvaluationDTO.EducationMatchDTO(
                matchScore.getEducationMatch().getRequiredDegree(),
                matchScore.getEducationMatch().getCandidateDegree(),
                matchScore.getEducationMatch().getMatchLevel(),
                matchScore.getEducationMatch().getReasoning()
        );
        List<TechnicalQuestionDTO> technicalQuestions = evaluation.getTechnicalQuestions() == null
                ? List.of()
                : evaluation.getTechnicalQuestions().stream()
                .map(question -> new TechnicalQuestionDTO(
                        question.getQuestion(),
                        question.getExpectedAnswer(),
                        question.getDifficulty(),
                        question.getSkillArea(),
                        question.getBluffIndicator(),
                        question.getFollowUpQuestions() == null
                                ? List.of()
                                : question.getFollowUpQuestions().stream().map(item -> item.getText()).toList()
                ))
                .toList();
        List<HrQuestionDTO> hrQuestions = evaluation.getHrQuestions() == null
                ? List.of()
                : evaluation.getHrQuestions().stream()
                .map(question -> new HrQuestionDTO(
                        question.getQuestion(),
                        question.getPsychologicalIntent(),
                        question.getIdealResponseIndicators() == null
                                ? List.of()
                                : question.getIdealResponseIndicators().stream().map(item -> item.getText()).toList(),
                        question.getRedFlags() == null
                                ? List.of()
                                : question.getRedFlags().stream().map(item -> item.getText()).toList(),
                        question.getFollowUpProbes() == null
                                ? List.of()
                                : question.getFollowUpProbes().stream().map(item -> item.getText()).toList(),
                        question.getEvaluationCriteria()
                ))
                .toList();
        HrEvaluationDetailDTO.ProfileDataDTO profileData = parseProfileData(cv == null ? null : cv.getProfileData());

        return new HrEvaluationDetailDTO(
                evaluation.getId(),
                evaluation.getStatus(),
                overallScore,
                recommendation,
                reasoning,
                matchedSkills,
                missingSkills,
                experienceAlignment,
                educationMatch,
                cv == null ? null : cv.getId(),
                cv == null ? null : cv.getUploadDate(),
                cv == null || cv.getCandidate() == null ? null : cv.getCandidate().getId(),
                cv == null || cv.getCandidate() == null ? null : cv.getCandidate().getFirstName() + " " + cv.getCandidate().getLastName(),
                cv == null || cv.getJobOffer() == null ? null : cv.getJobOffer().getId(),
                cv == null || cv.getJobOffer() == null ? null : cv.getJobOffer().getTitle(),
                profileData,
                technicalQuestions,
                hrQuestions
        );
    }

    private HrEvaluationDetailDTO.ProfileDataDTO parseProfileData(ProfileData profileData) {
        if (profileData == null) {
            return new HrEvaluationDetailDTO.ProfileDataDTO(
                    new HrEvaluationDetailDTO.PersonalInfoDTO(null, null, null, null),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        HrEvaluationDetailDTO.PersonalInfoDTO personalInfoDTO = new HrEvaluationDetailDTO.PersonalInfoDTO(
                firstNotBlank(
                        profileData.getPersonalInfo() == null ? null : profileData.getPersonalInfo().getEmail(),
                        profileData.getContactInfo() == null ? null : profileData.getContactInfo().getEmail()
                ),
                firstNotBlank(
                        profileData.getPersonalInfo() == null ? null : profileData.getPersonalInfo().getPhone(),
                        profileData.getContactInfo() == null ? null : profileData.getContactInfo().getPhone()
                ),
                profileData.getPersonalInfo() == null ? null : trimToNull(profileData.getPersonalInfo().getLocation()),
                profileData.getContactInfo() == null ? null : trimToNull(profileData.getContactInfo().getLinkedin())
        );

        List<HrEvaluationDetailDTO.ExperienceItemDTO> experience = profileData.getExperiences() == null
                ? List.of()
                : profileData.getExperiences().stream().map(item -> {
                    String duration = null;
                    String startDate = trimToNull(item.getStartDate());
                    String endDate = trimToNull(item.getEndDate());
                    if (startDate != null || endDate != null) {
                        duration = (startDate == null ? "?" : startDate) + " - " + (endDate == null ? "Present" : endDate);
                    }
                    return new HrEvaluationDetailDTO.ExperienceItemDTO(
                            trimToNull(item.getTitle()),
                            trimToNull(item.getCompany()),
                            duration,
                            trimToNull(item.getDescription())
                    );
                }).filter(item ->
                        item.title() != null || item.company() != null || item.duration() != null || item.description() != null
                ).toList();

        List<HrEvaluationDetailDTO.EducationItemDTO> education = profileData.getEducation() == null
                ? List.of()
                : profileData.getEducation().stream()
                .map(item -> new HrEvaluationDetailDTO.EducationItemDTO(
                        trimToNull(item.getDegree()),
                        trimToNull(item.getInstitution()),
                        firstNotBlank(item.getEndDate(), item.getStartDate())
                ))
                .filter(item -> item.degree() != null || item.institution() != null || item.year() != null)
                .toList();

        List<String> languages = profileData.getLanguages() == null
                ? List.of()
                : profileData.getLanguages().stream()
                .map(item -> {
                    String language = trimToNull(item.getLanguage());
                    String level = trimToNull(item.getLevel());
                    if (language == null) return null;
                    return level == null ? language : language + " (" + level + ")";
                })
                .filter(item -> item != null && !item.isBlank())
                .toList();

        List<String> skills = profileData.getSkills() == null
                ? List.of()
                : profileData.getSkills().stream()
                .map(item -> trimToNull(item.getName()))
                .filter(item -> item != null && !item.isBlank())
                .toList();

        return new HrEvaluationDetailDTO.ProfileDataDTO(personalInfoDTO, experience, education, languages, skills);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNotBlank(String first, String second) {
        String firstValue = trimToNull(first);
        if (firstValue != null) {
            return firstValue;
        }
        return trimToNull(second);
    }

    private Integer toIntegerYears(Double value) {
        if (value == null) {
            return null;
        }
        long rounded = Math.round(value);
        if (rounded < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

}

package org.djezzy.pfe.service.system;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dto.evaluation.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.N8nEvaluationPayloadDTO;
import org.djezzy.pfe.dto.job.StructuredJdCallbackRequest;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.Certificate;
import org.djezzy.pfe.model.evaluation.ContactInfo;
import org.djezzy.pfe.model.evaluation.Education;
import org.djezzy.pfe.model.evaluation.EducationMatch;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.model.evaluation.Experience;
import org.djezzy.pfe.model.evaluation.ExperienceAlignment;
import org.djezzy.pfe.model.evaluation.FollowUpProbe;
import org.djezzy.pfe.model.evaluation.FollowUpQuestion;
import org.djezzy.pfe.model.evaluation.HRQuestion;
import org.djezzy.pfe.model.evaluation.Hobby;
import org.djezzy.pfe.model.evaluation.IdealResponseIndicator;
import org.djezzy.pfe.model.evaluation.Language;
import org.djezzy.pfe.model.evaluation.MatchedSkill;
import org.djezzy.pfe.model.evaluation.MatchScore;
import org.djezzy.pfe.model.evaluation.MissingSkill;
import org.djezzy.pfe.model.evaluation.NormalizedSkill;
import org.djezzy.pfe.model.evaluation.PersonalInfo;
import org.djezzy.pfe.model.evaluation.ProfileData;
import org.djezzy.pfe.model.evaluation.RedFlag;
import org.djezzy.pfe.model.evaluation.Skill;
import org.djezzy.pfe.model.evaluation.TechnicalQuestion;
import org.djezzy.pfe.service.job.JobOfferService;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallbackService {
    private final JobOfferService jobOfferService;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final CVDAO cvdao;
    private final MapperUtil mapperUtil;

    @Transactional
    public JobOfferDTO handleStructuredJdCallback(Long jobOfferId, StructuredJdCallbackRequest request) {
        return jobOfferService.applyStructuredJdCallback(jobOfferId, request);
    }

    @Transactional
    public CandidateEvaluationDTO handleEvaluationCallback(Long evaluationId, N8nEvaluationPayloadDTO payload) {
        CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Candidate evaluation not found"));
        CV cv = evaluation.getCv();
        if (cv == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Candidate evaluation is not linked to a CV");
        }

        mapProfileData(cv, payload.profileData());
        mapMatchScore(evaluation, payload.matchScore());
        mapTechnicalQuestions(evaluation, payload.technicalQuestions());
        mapHrQuestions(evaluation, payload.hrQuestions());

        evaluation.setStatus(EvaluationStatus.SCORED);
        cv.setStatus(CVProcessingStatus.EVALUATED);

        candidateEvaluationDAO.save(evaluation);
        cvdao.save(cv);
        return mapperUtil.toCandidateEvaluationDto(evaluation);
    }

    private void mapProfileData(CV cv, N8nEvaluationPayloadDTO.ProfileDataDTO payload) {
        if (payload == null) {
            return;
        }
        ProfileData profileData = cv.getProfileData() == null ? new ProfileData() : cv.getProfileData();
        mapPersonalInfo(profileData, payload.personalInfo());
        mapExperiences(profileData, payload.experiences());
        mapEducation(profileData, payload.education());
        mapSkills(profileData, payload.skills());
        mapLanguages(profileData, payload.languages());
        mapCertificates(profileData, payload.certificates());
        mapHobbies(profileData, payload.hobbies());
        mapContactInfo(profileData, payload.contactInfo());
        mapNormalizedSkills(profileData, payload.normalizedSkills());
        cv.setProfileData(profileData);
    }

    private void mapPersonalInfo(ProfileData profileData, N8nEvaluationPayloadDTO.PersonalInfoDTO payload) {
        if (payload == null) {
            return;
        }
        PersonalInfo personalInfo = profileData.getPersonalInfo() == null ? new PersonalInfo() : profileData.getPersonalInfo();
        personalInfo.setFirstName(payload.firstName());
        personalInfo.setLastName(payload.lastName());
        personalInfo.setEmail(payload.email());
        personalInfo.setPhone(payload.phone());
        personalInfo.setLocation(payload.location());
        profileData.setPersonalInfo(personalInfo);
    }

    private void mapExperiences(ProfileData profileData, List<N8nEvaluationPayloadDTO.ExperienceDTO> experiences) {
        profileData.clearExperiences();
        if (experiences == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.ExperienceDTO payload : experiences) {
            if (payload == null) {
                continue;
            }
            Experience experience = new Experience();
            experience.setTitle(payload.title());
            experience.setCompany(payload.company());
            experience.setStartDate(payload.startDate());
            experience.setEndDate(payload.endDate());
            experience.setDescription(payload.description());
            profileData.addExperience(experience);
        }
    }

    private void mapEducation(ProfileData profileData, List<N8nEvaluationPayloadDTO.EducationDTO> education) {
        profileData.clearEducation();
        if (education == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.EducationDTO payload : education) {
            if (payload == null) {
                continue;
            }
            Education educationEntry = new Education();
            educationEntry.setDegree(payload.degree());
            educationEntry.setInstitution(payload.institution());
            educationEntry.setStartDate(payload.startDate());
            educationEntry.setEndDate(payload.endDate());
            educationEntry.setHonors(payload.honors());
            profileData.addEducation(educationEntry);
        }
    }

    private void mapSkills(ProfileData profileData, List<String> skills) {
        profileData.clearSkills();
        if (skills == null) {
            return;
        }
        for (String skillName : skills) {
            if (!hasText(skillName)) {
                continue;
            }
            profileData.addSkill(new Skill(skillName.trim()));
        }
    }

    private void mapLanguages(ProfileData profileData, List<N8nEvaluationPayloadDTO.LanguageDTO> languages) {
        profileData.clearLanguages();
        if (languages == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.LanguageDTO payload : languages) {
            if (payload == null) {
                continue;
            }
            Language language = new Language();
            language.setLanguage(payload.language());
            language.setLevel(payload.level());
            profileData.addLanguage(language);
        }
    }

    private void mapCertificates(ProfileData profileData, List<N8nEvaluationPayloadDTO.CertificateDTO> certificates) {
        profileData.clearCertificates();
        if (certificates == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.CertificateDTO payload : certificates) {
            if (payload == null) {
                continue;
            }
            Certificate certificate = new Certificate();
            certificate.setName(payload.name());
            certificate.setIssuer(payload.issuer());
            certificate.setDate(payload.date());
            profileData.addCertificate(certificate);
        }
    }

    private void mapHobbies(ProfileData profileData, List<String> hobbies) {
        profileData.clearHobbies();
        if (hobbies == null) {
            return;
        }
        for (String hobbyName : hobbies) {
            if (!hasText(hobbyName)) {
                continue;
            }
            profileData.addHobby(new Hobby(hobbyName.trim()));
        }
    }

    private void mapContactInfo(ProfileData profileData, N8nEvaluationPayloadDTO.ContactInfoDTO payload) {
        if (payload == null) {
            return;
        }
        ContactInfo contactInfo = profileData.getContactInfo() == null ? new ContactInfo() : profileData.getContactInfo();
        contactInfo.setEmail(payload.email());
        contactInfo.setPhone(payload.phone());
        contactInfo.setLinkedin(payload.linkedin());
        profileData.setContactInfo(contactInfo);
    }

    private void mapNormalizedSkills(ProfileData profileData, List<N8nEvaluationPayloadDTO.NormalizedSkillDTO> normalizedSkills) {
        profileData.clearNormalizedSkills();
        if (normalizedSkills == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.NormalizedSkillDTO payload : normalizedSkills) {
            if (payload == null) {
                continue;
            }
            NormalizedSkill normalizedSkill = new NormalizedSkill();
            normalizedSkill.setOriginalName(payload.originalName());
            normalizedSkill.setNormalizedName(payload.normalizedName());
            normalizedSkill.setCategory(payload.category());
            normalizedSkill.setProficiencyLevel(payload.proficiencyLevel());
            normalizedSkill.setYearsExperience(payload.yearsExperience());
            profileData.addNormalizedSkill(normalizedSkill);
        }
    }

    private void mapMatchScore(CandidateEvaluation evaluation, N8nEvaluationPayloadDTO.MatchScoreDTO payload) {
        if (payload == null) {
            return;
        }
        MatchScore matchScore = evaluation.getMatchScore() == null ? new MatchScore() : evaluation.getMatchScore();
        matchScore.setOverallScore(payload.overallScore());
        matchScore.setRecommendation(payload.recommendation());
        matchScore.setReasoning(payload.reasoning());
        mapMatchedSkills(matchScore, payload.matchedSkills());
        mapMissingSkills(matchScore, payload.missingSkills());
        mapExperienceAlignment(matchScore, payload.experienceAlignment());
        mapEducationMatch(matchScore, payload.educationMatch());
        evaluation.setMatchScore(matchScore);
    }

    private void mapMatchedSkills(MatchScore matchScore, List<String> matchedSkills) {
        matchScore.clearMatchedSkills();
        if (matchedSkills == null) {
            return;
        }
        for (String skillName : matchedSkills) {
            if (!hasText(skillName)) {
                continue;
            }
            matchScore.addMatchedSkill(new MatchedSkill(skillName.trim()));
        }
    }

    private void mapMissingSkills(MatchScore matchScore, List<N8nEvaluationPayloadDTO.MissingSkillDTO> missingSkills) {
        matchScore.clearMissingSkills();
        if (missingSkills == null) {
            return;
        }
        for (N8nEvaluationPayloadDTO.MissingSkillDTO payload : missingSkills) {
            if (payload == null || !hasText(payload.skillName())) {
                continue;
            }
            matchScore.addMissingSkill(new MissingSkill(payload.skillName().trim(), payload.importance()));
        }
    }

    private void mapExperienceAlignment(MatchScore matchScore, N8nEvaluationPayloadDTO.ExperienceAlignmentDTO payload) {
        if (payload == null) {
            return;
        }
        ExperienceAlignment experienceAlignment = matchScore.getExperienceAlignment() == null
                ? new ExperienceAlignment()
                : matchScore.getExperienceAlignment();
        experienceAlignment.setYearsRequired(payload.yearsRequired());
        experienceAlignment.setYearsCandidate(payload.yearsCandidate());
        experienceAlignment.setMatchPercentage(payload.matchPercentage());
        matchScore.setExperienceAlignment(experienceAlignment);
    }

    private void mapEducationMatch(MatchScore matchScore, N8nEvaluationPayloadDTO.EducationMatchDTO payload) {
        if (payload == null) {
            return;
        }
        EducationMatch educationMatch = matchScore.getEducationMatch() == null
                ? new EducationMatch()
                : matchScore.getEducationMatch();
        educationMatch.setRequiredDegree(payload.requiredDegree());
        educationMatch.setCandidateDegree(payload.candidateDegree());
        educationMatch.setMatchStatus(payload.matchStatus());
        matchScore.setEducationMatch(educationMatch);
    }

    private void mapTechnicalQuestions(CandidateEvaluation evaluation, List<N8nEvaluationPayloadDTO.TechnicalQuestionDTO> technicalQuestions) {
        if (technicalQuestions == null) {
            return;
        }
        evaluation.clearTechnicalQuestions();
        for (N8nEvaluationPayloadDTO.TechnicalQuestionDTO payload : technicalQuestions) {
            if (payload == null) {
                continue;
            }
            TechnicalQuestion technicalQuestion = new TechnicalQuestion();
            technicalQuestion.setQuestion(payload.question());
            technicalQuestion.setExpectedAnswer(payload.expectedAnswer());
            technicalQuestion.setDifficulty(payload.difficulty());
            technicalQuestion.setSkillArea(payload.skillArea());
            technicalQuestion.setBluffIndicator(payload.bluffIndicator());
            mapFollowUpQuestions(technicalQuestion, payload.followUpQuestions());
            evaluation.addTechnicalQuestion(technicalQuestion);
        }
    }

    private void mapFollowUpQuestions(TechnicalQuestion technicalQuestion, List<String> followUpQuestions) {
        technicalQuestion.clearFollowUpQuestions();
        if (followUpQuestions == null) {
            return;
        }
        for (String text : followUpQuestions) {
            if (!hasText(text)) {
                continue;
            }
            FollowUpQuestion followUpQuestion = new FollowUpQuestion();
            followUpQuestion.setText(text.trim());
            technicalQuestion.addFollowUpQuestion(followUpQuestion);
        }
    }

    private void mapHrQuestions(CandidateEvaluation evaluation, List<N8nEvaluationPayloadDTO.HrQuestionDTO> hrQuestions) {
        if (hrQuestions == null) {
            return;
        }
        evaluation.clearHrQuestions();
        for (N8nEvaluationPayloadDTO.HrQuestionDTO payload : hrQuestions) {
            if (payload == null) {
                continue;
            }
            HRQuestion hrQuestion = new HRQuestion();
            hrQuestion.setQuestion(payload.question());
            hrQuestion.setPsychologicalIntent(payload.psychologicalIntent());
            hrQuestion.setEvaluationCriteria(payload.evaluationCriteria());
            mapIdealResponseIndicators(hrQuestion, payload.idealResponseIndicators());
            mapRedFlags(hrQuestion, payload.redFlags());
            mapFollowUpProbes(hrQuestion, payload.followUpProbes());
            evaluation.addHrQuestion(hrQuestion);
        }
    }

    private void mapIdealResponseIndicators(HRQuestion hrQuestion, List<String> indicators) {
        hrQuestion.clearIdealResponseIndicators();
        if (indicators == null) {
            return;
        }
        for (String text : indicators) {
            if (!hasText(text)) {
                continue;
            }
            IdealResponseIndicator indicator = new IdealResponseIndicator();
            indicator.setText(text.trim());
            hrQuestion.addIdealResponseIndicator(indicator);
        }
    }

    private void mapRedFlags(HRQuestion hrQuestion, List<String> redFlags) {
        hrQuestion.clearRedFlags();
        if (redFlags == null) {
            return;
        }
        for (String text : redFlags) {
            if (!hasText(text)) {
                continue;
            }
            RedFlag redFlag = new RedFlag();
            redFlag.setText(text.trim());
            hrQuestion.addRedFlag(redFlag);
        }
    }

    private void mapFollowUpProbes(HRQuestion hrQuestion, List<String> probes) {
        hrQuestion.clearFollowUpProbes();
        if (probes == null) {
            return;
        }
        for (String text : probes) {
            if (!hasText(text)) {
                continue;
            }
            FollowUpProbe followUpProbe = new FollowUpProbe();
            followUpProbe.setText(text.trim());
            hrQuestion.addFollowUpProbe(followUpProbe);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}



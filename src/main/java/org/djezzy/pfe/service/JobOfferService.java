package org.djezzy.pfe.service;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.JobOfferDAO;
import org.djezzy.pfe.dao.StructuredJdDAO;
import org.djezzy.pfe.dto.CreateJobOfferRequest;
import org.djezzy.pfe.dto.ExperienceRangeDTO;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.StructuredJdCallbackRequest;
import org.djezzy.pfe.dto.UpdateJobOfferRequest;
import org.djezzy.pfe.model.ExperienceRange;
import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.model.JobOfferStatus;
import org.djezzy.pfe.model.PreferredSkill;
import org.djezzy.pfe.model.Qualification;
import org.djezzy.pfe.model.RequiredSkill;
import org.djezzy.pfe.model.Responsibility;
import org.djezzy.pfe.model.StructuredJd;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobOfferService {
    private final JobOfferDAO jobOfferDAO;
    private final StructuredJdDAO structuredJdDAO;
    private final AsyncWorkflowService asyncWorkflowService;
    private final MapperUtil mapperUtil;

    @Transactional
    public JobOfferDTO createJobOffer(CreateJobOfferRequest request, User actor) {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setTitle(request.title());
        jobOffer.setRawText(request.rawText());
        jobOffer.setStatus(JobOfferStatus.STRUCTURING);
        jobOffer.setCreatedBy(actor);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        asyncWorkflowService.triggerStructuredJdWorkflow(jobOffer.getId());
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional
    public JobOfferDTO updateJobOffer(Long jobOfferId, UpdateJobOfferRequest request) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOffer.setTitle(request.title());
        jobOffer.setRawText(request.rawText());
        jobOffer.setStatus(JobOfferStatus.STRUCTURING);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        asyncWorkflowService.triggerStructuredJdWorkflow(jobOffer.getId());
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional
    public void deleteJobOffer(Long jobOfferId) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOfferDAO.delete(jobOffer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listPublicJobOffers() {
        List<JobOffer> offers = new ArrayList<>();
        offers.addAll(jobOfferDAO.findByStatus(JobOfferStatus.STRUCTURED));
        offers.addAll(jobOfferDAO.findByStatus(JobOfferStatus.ACTIVE));
        return mapperUtil.toJobOfferDtos(offers);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listAllJobOffers() {
        return mapperUtil.toJobOfferDtos(jobOfferDAO.findAll());
    }

    @Transactional
    public JobOfferDTO applyStructuredJdCallback(Long jobOfferId, StructuredJdCallbackRequest request) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        StructuredJd structuredJd = structuredJdDAO.findByJobOfferId(jobOfferId).orElse(new StructuredJd());
        structuredJd.setJobOffer(jobOffer);
        structuredJd.setTitle(request.title());
        structuredJd.setCompanyName(request.companyName());
        structuredJd.setWorkLocation(request.workLocation());

        ExperienceRangeDTO rangeDTO = request.experienceRange();
        if (rangeDTO != null) {
            ExperienceRange range = structuredJd.getExperienceRange() == null ? new ExperienceRange() : structuredJd.getExperienceRange();
            range.setMinYears(rangeDTO.minYears());
            range.setMaxYears(rangeDTO.maxYears());
            structuredJd.setExperienceRange(range);
        }

        structuredJd.getRequiredSkills().clear();
        if (request.requiredSkills() != null) {
            request.requiredSkills().forEach(skillName -> {
                RequiredSkill skill = new RequiredSkill();
                skill.setName(skillName);
                skill.setStructuredJd(structuredJd);
                structuredJd.getRequiredSkills().add(skill);
            });
        }

        structuredJd.getPreferredSkills().clear();
        if (request.preferredSkills() != null) {
            request.preferredSkills().forEach(skillName -> {
                PreferredSkill skill = new PreferredSkill();
                skill.setName(skillName);
                skill.setStructuredJd(structuredJd);
                structuredJd.getPreferredSkills().add(skill);
            });
        }

        structuredJd.getResponsibilities().clear();
        if (request.responsibilities() != null) {
            request.responsibilities().forEach(value -> {
                Responsibility responsibility = new Responsibility();
                responsibility.setDescription(value);
                responsibility.setStructuredJd(structuredJd);
                structuredJd.getResponsibilities().add(responsibility);
            });
        }

        structuredJd.getQualifications().clear();
        if (request.qualifications() != null) {
            request.qualifications().forEach(value -> {
                Qualification qualification = new Qualification();
                qualification.setDescription(value);
                qualification.setStructuredJd(structuredJd);
                structuredJd.getQualifications().add(qualification);
            });
        }

        structuredJdDAO.save(structuredJd);
        jobOffer.setStructuredJd(structuredJd);
        jobOffer.setStatus(JobOfferStatus.STRUCTURED);
        jobOfferDAO.save(jobOffer);
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional(readOnly = true)
    public JobOffer findJobOffer(Long jobOfferId) {
        return jobOfferDAO.findById(jobOfferId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Job offer not found"));
    }
}

package org.djezzy.pfe.service.job;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.job.JobOfferDAO;
import org.djezzy.pfe.dao.job.StructuredJdDAO;
import org.djezzy.pfe.dto.job.CreateJobOfferRequest;
import org.djezzy.pfe.dto.job.ExperienceRangeDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.StructuredJdCallbackRequest;
import org.djezzy.pfe.dto.job.TestJobOfferCreationDTO;
import org.djezzy.pfe.dto.job.UpdateJobOfferRequest;
import org.djezzy.pfe.model.job.ExperienceRange;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.djezzy.pfe.model.job.PreferredSkill;
import org.djezzy.pfe.model.job.Qualification;
import org.djezzy.pfe.model.job.RequiredSkill;
import org.djezzy.pfe.model.job.Responsibility;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.evaluation.AsyncWorkflowService;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setCreatedBy(actor);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        asyncWorkflowService.triggerStructuredJdWorkflow(
                jobOffer.getId(),
                jobOffer.getJdRequestId(),
                jobOffer.getTitle(),
                jobOffer.getRawText()
        );
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional
    public JobOfferDTO createTestJobOfferWithStructuredJd(TestJobOfferCreationDTO request, User actor) {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setTitle(request.title());
        jobOffer.setRawText(request.rawText());
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setCreatedBy(actor);
        jobOfferDAO.save(jobOffer);

        StructuredJdCallbackRequest structuredRequest = new StructuredJdCallbackRequest(
                request.title(),
                request.companyName(),
                request.requiredSkills(),
                request.preferredSkills(),
                request.experienceRange(),
                List.of(),
                List.of(),
                request.workLocation()
        );
        return applyStructuredJdCallback(jobOffer.getId(), structuredRequest);
    }

    @Transactional
    public JobOfferDTO updateJobOffer(Long jobOfferId, UpdateJobOfferRequest request) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOffer.setTitle(request.title());
        jobOffer.setRawText(request.rawText());
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        asyncWorkflowService.triggerStructuredJdWorkflow(
                jobOffer.getId(),
                jobOffer.getJdRequestId(),
                jobOffer.getTitle(),
                jobOffer.getRawText()
        );
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional
    public void deleteJobOffer(Long jobOfferId) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOfferDAO.delete(jobOffer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listPublicJobOffers() {
        return mapperUtil.toJobOfferDtos(jobOfferDAO.findByStatus(JobOfferStatus.PUBLISHED));
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> listPublicJobOffers(Pageable pageable, String location) {
        Specification<JobOffer> specification = Specification.where((root, query, cb) -> cb.equal(root.get("status"), JobOfferStatus.PUBLISHED));
        if (location != null && !location.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.join("structuredJd", jakarta.persistence.criteria.JoinType.LEFT).get("workLocation")),
                            "%" + location.toLowerCase() + "%"
                    )
            );
        }
        return jobOfferDAO.findAll(specification, pageable).map(mapperUtil::toJobOfferDto);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listAllJobOffers() {
        return mapperUtil.toJobOfferDtos(jobOfferDAO.findAll());
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> listHrJobOffers(Pageable pageable, String location, LocalDate dateCreated, JobOfferStatus status) {
        Specification<JobOffer> specification = Specification.where(null);
        if (location != null && !location.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.join("structuredJd", jakarta.persistence.criteria.JoinType.LEFT).get("workLocation")),
                            "%" + location.toLowerCase() + "%"
                    )
            );
        }
        if (dateCreated != null) {
            Instant startOfDay = dateCreated.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant startOfNextDay = dateCreated.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, cb) ->
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay),
                            cb.lessThan(root.get("createdAt"), startOfNextDay)
                    )
            );
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return jobOfferDAO.findAll(specification, pageable).map(mapperUtil::toJobOfferDto);
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

        structuredJd.clearRequiredSkills();
        if (request.requiredSkills() != null) {
            request.requiredSkills().forEach(skillName -> {
                RequiredSkill skill = new RequiredSkill();
                skill.setName(skillName);
                structuredJd.addRequiredSkill(skill);
            });
        }

        structuredJd.clearPreferredSkills();
        if (request.preferredSkills() != null) {
            request.preferredSkills().forEach(skillName -> {
                PreferredSkill skill = new PreferredSkill();
                skill.setName(skillName);
                structuredJd.addPreferredSkill(skill);
            });
        }

        structuredJd.clearResponsibilities();
        if (request.responsibilities() != null) {
            request.responsibilities().forEach(value -> {
                Responsibility responsibility = new Responsibility();
                responsibility.setDescription(value);
                structuredJd.addResponsibility(responsibility);
            });
        }

        structuredJd.clearQualifications();
        if (request.qualifications() != null) {
            request.qualifications().forEach(value -> {
                Qualification qualification = new Qualification();
                qualification.setDescription(value);
                structuredJd.addQualification(qualification);
            });
        }

        structuredJdDAO.save(structuredJd);
        jobOffer.setStructuredJd(structuredJd);
        jobOffer.setStatus(JobOfferStatus.PUBLISHED);
        jobOfferDAO.save(jobOffer);
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional(readOnly = true)
    public JobOffer findJobOffer(Long jobOfferId) {
        return jobOfferDAO.findById(jobOfferId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Job offer not found"));
    }
}



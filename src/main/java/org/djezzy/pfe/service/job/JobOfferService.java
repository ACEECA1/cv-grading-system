package org.djezzy.pfe.service.job;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.job.JobOfferDAO;
import org.djezzy.pfe.dao.job.StructuredJdDAO;
import org.djezzy.pfe.dto.job.ApplicantSummaryDTO;
import org.djezzy.pfe.dto.job.CreateJobOfferRequest;
import org.djezzy.pfe.dto.job.ExperienceRangeDTO;
import org.djezzy.pfe.dto.job.JobOfferDetailDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.job.StructuredJdDTO;
import org.djezzy.pfe.dto.job.StructuredJdCallbackRequest;
import org.djezzy.pfe.dto.job.TestJobOfferCreationDTO;
import org.djezzy.pfe.dto.job.UpdateJobOfferRequest;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.job.ExperienceRange;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.djezzy.pfe.model.evaluation.MatchScore;
import org.djezzy.pfe.model.job.PreferredSkill;
import org.djezzy.pfe.model.job.Qualification;
import org.djezzy.pfe.model.job.RequiredSkill;
import org.djezzy.pfe.model.job.Responsibility;
import org.djezzy.pfe.model.job.StructuredJd;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobOfferService {
    private final CVDAO cvdao;
    private final JobOfferDAO jobOfferDAO;
    private final StructuredJdDAO structuredJdDAO;
    private final LlmParsingService llmParsingService;
    private final MapperUtil mapperUtil;
    private final AppProperties appProperties;
    private final ObjectProvider<JobOfferService> selfProvider;

    @Transactional
    public JobOfferDTO createJobOffer(CreateJobOfferRequest request, User actor) {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setTitle(request.title());
        jobOffer.setRawText(request.rawText());
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setCreatedBy(actor);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        triggerStructuredJdParsing(jobOffer);
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
                request.workLocation(),
                request.employmentType()
        );
        return applyStructuredJdCallback(jobOffer.getId(), structuredRequest);
    }

    @Transactional
    public JobOfferDetailDTO updateJobOffer(Long jobOfferId, UpdateJobOfferRequest request) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        UpdateJobOfferRequest.StructuredJdUpdateRequest structuredRequest = request.structuredJd();

        jobOffer.setTitle(request.title().trim());

        StructuredJd structuredJd = structuredJdDAO.findByJobOfferId(jobOfferId).orElse(new StructuredJd());
        structuredJd.setJobOffer(jobOffer);

        List<String> sanitizedResponsibilities = normalizeTextList(structuredRequest.responsibilities());
        List<String> preservedQualifications = structuredJd.getQualifications() == null
                ? List.of()
                : structuredJd.getQualifications().stream()
                .map(Qualification::getDescription)
                .toList();

        applyStructuredJdData(
                structuredJd,
                new StructuredJdCallbackRequest(
                        request.title().trim(),
                        structuredJd.getCompanyName(),
                        normalizeTextList(structuredRequest.requiredSkills()),
                        normalizeTextList(structuredRequest.preferredSkills()),
                        structuredRequest.experienceRange(),
                        sanitizedResponsibilities,
                        preservedQualifications,
                        trimToNull(structuredRequest.workLocation()),
                        trimToNull(structuredRequest.employmentType())
                )
        );

        structuredJdDAO.save(structuredJd);
        jobOffer.setStructuredJd(structuredJd);
        if (!sanitizedResponsibilities.isEmpty()) {
            jobOffer.setRawText(String.join(System.lineSeparator(), sanitizedResponsibilities));
        } else if (!hasText(jobOffer.getRawText())) {
            jobOffer.setRawText(request.title().trim());
        }
        jobOfferDAO.save(jobOffer);
        return toJobOfferDetailDto(jobOffer);
    }

    @Transactional
    public JobOfferDetailDTO toggleJobStatus(Long jobOfferId, JobOfferStatus status) {
        if (status != JobOfferStatus.PUBLISHED && status != JobOfferStatus.CLOSED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Status must be either PUBLISHED or CLOSED");
        }
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOffer.setStatus(status);
        jobOfferDAO.save(jobOffer);
        return toJobOfferDetailDto(jobOffer);
    }

    @Transactional
    public JobOfferDTO retryJobDescriptionProcessing(Long jobOfferId) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        if (jobOffer.getStatus() == JobOfferStatus.PUBLISHED || jobOffer.getStatus() == JobOfferStatus.CLOSED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Job offer cannot be retried in its current status");
        }
        jobOffer.setStatus(JobOfferStatus.DRAFT);
        jobOffer.setJdRequestId(UUID.randomUUID().toString());
        jobOfferDAO.save(jobOffer);
        triggerStructuredJdParsing(jobOffer);
        return mapperUtil.toJobOfferDto(jobOffer);
    }

    @Transactional
    public void deleteJobOffer(Long jobOfferId) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        jobOfferDAO.delete(jobOffer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listPublicJobOffers() {
        return jobOfferDAO.findByStatus(JobOfferStatus.PUBLISHED).stream()
                .map(this::toPublicJobOfferDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> listPublicJobOffers(Pageable pageable, String location) {
        return getPublishedJobOffers(null, location, pageable);
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getPublishedJobOffers(String title, String location, Pageable pageable) {
        return jobOfferDAO.findAllByFilters(title, location, true, pageable).map(this::toPublicJobOfferDto);
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> listAllJobOffers() {
        return mapperUtil.toJobOfferDtos(jobOfferDAO.findAll());
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getJobOffers(
            String title,
            String location,
            Boolean isPublished,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid sort direction. Use 'asc' or 'desc'.");
        }
        String sortProperty = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        return jobOfferDAO.findAllByFilters(title, location, isPublished, pageable).map(mapperUtil::toJobOfferDto);
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

    @Transactional(readOnly = true)
    public JobOfferDetailDTO getJobOffer(Long jobOfferId) {
        return toJobOfferDetailDto(findJobOffer(jobOfferId));
    }

    @Transactional(readOnly = true)
    public JobOfferDetailDTO getPublicJobOffer(Long jobOfferId) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        if (jobOffer.getStatus() != JobOfferStatus.PUBLISHED) {
            throw new AppException(HttpStatus.NOT_FOUND, "Job offer not found");
        }
        return toPublicJobOfferDetailDto(jobOffer);
    }

    @Transactional(readOnly = true)
    public List<ApplicantSummaryDTO> getJobApplicants(Long jobOfferId) {
        findJobOffer(jobOfferId);
        return cvdao.findByJobOfferIdOrderByUploadDateDesc(jobOfferId).stream()
                .map(this::toApplicantSummaryDto)
                .toList();
    }

    @Transactional
    public JobOfferDTO applyStructuredJdCallback(Long jobOfferId, StructuredJdCallbackRequest request) {
        JobOffer jobOffer = findJobOffer(jobOfferId);
        StructuredJd structuredJd = structuredJdDAO.findByJobOfferId(jobOfferId).orElse(new StructuredJd());
        structuredJd.setJobOffer(jobOffer);
        applyStructuredJdData(structuredJd, request);
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

    @Async("applicationTaskExecutor")
    public void processJobDescriptionAsync(Long jobOfferId, String requestId) {
        JobOfferSnapshot snapshot = self().loadDraftSnapshot(jobOfferId, requestId);
        if (snapshot == null) {
            return;
        }
        try {
            StructuredJd parsedStructuredJd = llmParsingService.parseStructuredJd(snapshot.rawText());
            self().publishParsedStructuredJd(jobOfferId, requestId, parsedStructuredJd);
        } catch (Exception ex) {
            log.error("Structured JD parsing failed for job offer {}", jobOfferId, ex);
            self().markJobOfferAsFailed(jobOfferId, requestId);
        }
    }

    @Transactional(readOnly = true)
    public JobOfferSnapshot loadDraftSnapshot(Long jobOfferId, String requestId) {
        JobOffer jobOffer = jobOfferDAO.findById(jobOfferId).orElse(null);
        if (jobOffer == null) {
            log.warn("Job offer {} not found for async parsing", jobOfferId);
            return null;
        }
        if (!isSameDraftAttempt(jobOffer, requestId)) {
            return null;
        }
        return new JobOfferSnapshot(jobOffer.getId(), jobOffer.getRawText());
    }

    @Transactional
    public void publishParsedStructuredJd(Long jobOfferId, String requestId, StructuredJd parsedStructuredJd) {
        JobOffer jobOffer = jobOfferDAO.findById(jobOfferId).orElse(null);
        if (jobOffer == null || !isSameDraftAttempt(jobOffer, requestId)) {
            return;
        }
        StructuredJd structuredJd = structuredJdDAO.findByJobOfferId(jobOfferId).orElse(new StructuredJd());
        structuredJd.setJobOffer(jobOffer);
        applyStructuredJdData(structuredJd, toCallbackRequest(parsedStructuredJd, jobOffer.getTitle()));
        structuredJdDAO.save(structuredJd);
        jobOffer.setStructuredJd(structuredJd);
        jobOffer.setStatus(JobOfferStatus.PUBLISHED);
        jobOfferDAO.save(jobOffer);
    }

    @Transactional
    public void markJobOfferAsFailed(Long jobOfferId, String requestId) {
        JobOffer jobOffer = jobOfferDAO.findById(jobOfferId).orElse(null);
        if (jobOffer == null || !isSameDraftAttempt(jobOffer, requestId)) {
            return;
        }
        jobOffer.setStatus(JobOfferStatus.FAILED);
        jobOfferDAO.save(jobOffer);
    }

    private void triggerStructuredJdParsing(JobOffer jobOffer) {
        Runnable dispatch = () -> self().processJobDescriptionAsync(jobOffer.getId(), jobOffer.getJdRequestId());
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }
        dispatch.run();
    }

    private JobOfferService self() {
        return selfProvider.getObject();
    }

    private boolean isSameDraftAttempt(JobOffer jobOffer, String requestId) {
        return requestId != null
                && requestId.equals(jobOffer.getJdRequestId())
                && jobOffer.getStatus() == JobOfferStatus.DRAFT;
    }

    private StructuredJdCallbackRequest toCallbackRequest(StructuredJd parsedStructuredJd, String fallbackTitle) {
        ExperienceRangeDTO rangeDTO = parsedStructuredJd.getExperienceRange() == null
                ? null
                : new ExperienceRangeDTO(
                parsedStructuredJd.getExperienceRange().getMinYears(),
                parsedStructuredJd.getExperienceRange().getMaxYears()
        );
        String title = parsedStructuredJd.getTitle() == null || parsedStructuredJd.getTitle().isBlank()
                ? fallbackTitle
                : parsedStructuredJd.getTitle();

        return new StructuredJdCallbackRequest(
                title,
                parsedStructuredJd.getCompanyName(),
                parsedStructuredJd.getRequiredSkills() == null ? List.of() : parsedStructuredJd.getRequiredSkills().stream().map(RequiredSkill::getName).toList(),
                parsedStructuredJd.getPreferredSkills() == null ? List.of() : parsedStructuredJd.getPreferredSkills().stream().map(PreferredSkill::getName).toList(),
                rangeDTO,
                parsedStructuredJd.getResponsibilities() == null ? List.of() : parsedStructuredJd.getResponsibilities().stream().map(Responsibility::getDescription).toList(),
                parsedStructuredJd.getQualifications() == null ? List.of() : parsedStructuredJd.getQualifications().stream().map(Qualification::getDescription).toList(),
                parsedStructuredJd.getWorkLocation(),
                parsedStructuredJd.getEmploymentType()
        );
    }

    private JobOfferDetailDTO toJobOfferDetailDto(JobOffer jobOffer) {
        return new JobOfferDetailDTO(
                jobOffer.getId(),
                jobOffer.getTitle(),
                jobOffer.getStatus(),
                jobOffer.getCreatedAt(),
                mapperUtil.toStructuredJdDto(jobOffer.getStructuredJd())
        );
    }

    private JobOfferDTO toPublicJobOfferDto(JobOffer jobOffer) {
        JobOfferDTO dto = mapperUtil.toJobOfferDto(jobOffer);
        return new JobOfferDTO(
                dto.id(),
                dto.title(),
                null,
                dto.status(),
                null,
                sanitizeStructuredJdForPublic(dto.structuredJd()),
                dto.createdAt(),
                null
        );
    }

    private JobOfferDetailDTO toPublicJobOfferDetailDto(JobOffer jobOffer) {
        JobOfferDetailDTO dto = toJobOfferDetailDto(jobOffer);
        return new JobOfferDetailDTO(
                dto.id(),
                dto.title(),
                dto.status(),
                dto.createdAt(),
                sanitizeStructuredJdForPublic(dto.structuredJd())
        );
    }

    private StructuredJdDTO sanitizeStructuredJdForPublic(StructuredJdDTO structuredJd) {
        if (structuredJd == null) {
            return null;
        }
        return new StructuredJdDTO(
                structuredJd.id(),
                structuredJd.title(),
                structuredJd.companyName(),
                List.of(),
                List.of(),
                structuredJd.experienceRange(),
                structuredJd.responsibilities(),
                structuredJd.qualifications(),
                structuredJd.workLocation(),
                structuredJd.employmentType()
        );
    }

    private ApplicantSummaryDTO toApplicantSummaryDto(CV cv) {
        CandidateEvaluation evaluation = cv.getCandidateEvaluation();
        MatchScore matchScore = evaluation == null ? null : evaluation.getMatchScore();
        Double rawScore = matchScore == null ? null : matchScore.getOverallScore();
        Double scoreAsPercent = rawScore == null
                ? null
                : Math.max(0.0, Math.min(rawScore > 10 ? rawScore : rawScore * 10, 100.0));
        String candidateName = cv.getCandidate() == null
                ? "Unknown candidate"
                : (trimToNull((cv.getCandidate().getFirstName() + " " + cv.getCandidate().getLastName())) == null
                ? "Unknown candidate"
                : trimToNull(cv.getCandidate().getFirstName() + " " + cv.getCandidate().getLastName()));
        String status = evaluation != null
                ? evaluation.getStatus().name()
                : cv.getStatus().name();

        return new ApplicantSummaryDTO(
                evaluation == null ? null : evaluation.getId(),
                candidateName,
                scoreAsPercent,
                status,
                cv.getUploadDate()
        );
    }

    private List<String> normalizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private void applyStructuredJdData(StructuredJd structuredJd, StructuredJdCallbackRequest request) {
        structuredJd.setTitle(request.title());
        String companyName = hasText(request.companyName()) ? request.companyName().trim() : appProperties.getCompanyName();
        structuredJd.setCompanyName(hasText(companyName) ? companyName.trim() : null);
        structuredJd.setWorkLocation(request.workLocation());
        structuredJd.setEmploymentType(request.employmentType());

        ExperienceRangeDTO rangeDTO = request.experienceRange();
        if (rangeDTO == null) {
            structuredJd.setExperienceRange(null);
        } else {
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
    }

    public record JobOfferSnapshot(Long jobOfferId, String rawText) {
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



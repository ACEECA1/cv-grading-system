package org.djezzy.pfe.service.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dto.evaluation.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.job.JobOfferDTO;
import org.djezzy.pfe.dto.evaluation.UploadCvResponseDTO;
import org.djezzy.pfe.event.CvUploadedEvent;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.auth.Candidate;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.model.job.JobOfferStatus;
import org.djezzy.pfe.model.auth.User;
import org.djezzy.pfe.service.job.JobOfferService;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.FileStorageUtil;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final CVDAO cvdao;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final JobOfferService jobOfferService;
    private final FileStorageUtil fileStorageUtil;
    private final MapperUtil mapperUtil;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public UploadCvResponseDTO uploadCv(Long jobOfferId, MultipartFile file, User user) {
        Candidate candidate = extractCandidate(user);

        JobOffer jobOffer = jobOfferService.findJobOffer(jobOfferId);
        if (jobOffer.getStatus() != JobOfferStatus.PUBLISHED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "This job offer is not open for submissions");
        }
        String filePath = fileStorageUtil.storePdf(file);

        CV cv = new CV();
        cv.setCandidate(candidate);
        cv.setJobOffer(jobOffer);
        cv.setFileUrl(filePath);
        cv.setUploadDate(Instant.now());
        cv.setStatus(CVProcessingStatus.UPLOADED);
        cvdao.save(cv);

        CandidateEvaluation evaluation = new CandidateEvaluation();
        evaluation.setCv(cv);
        evaluation.setStructuredJd(jobOffer.getStructuredJd());
        evaluation.setStatus(EvaluationStatus.WAITING);
        evaluation.setDetailsJson("{}");
        candidateEvaluationDAO.save(evaluation);
        cv.setCandidateEvaluation(evaluation);
        cvdao.save(cv);

        applicationEventPublisher.publishEvent(new CvUploadedEvent(cv.getId(), evaluation.getId()));

        return new UploadCvResponseDTO(
                cv.getId(),
                evaluation.getId(),
                cv.getStatus(),
                evaluation.getStatus(),
                cv.getUploadDate()
        );
    }

    @Transactional(readOnly = true)
    public List<CandidateSubmissionDTO> mySubmissions(User user) {
        extractCandidate(user);
        return mapperUtil.toCandidateSubmissionDtos(cvdao.findByCandidateIdOrderByUploadDateDesc(user.getId()));
    }

    @Transactional(readOnly = true)
    public Page<JobOfferDTO> browseJobOffers(Pageable pageable, String location) {
        return jobOfferService.listPublicJobOffers(pageable, location);
    }

    @Transactional(readOnly = true)
    public CV findLatestSubmission(Long jobOfferId, User user) {
        Candidate candidate = extractCandidate(user);
        return cvdao.findTopByCandidateIdAndJobOfferIdOrderByUploadDateDesc(candidate.getId(), jobOfferId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Submission not found for this job offer"));
    }

    @Transactional(readOnly = true)
    public Path resolveSubmissionCvPath(Long jobOfferId, User user) {
        CV cv = findLatestSubmission(jobOfferId, user);
        if (cv.getFileUrl() == null || cv.getFileUrl().isBlank()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Stored CV file path is missing");
        }
        Path path = fileStorageUtil.resolve(cv.getFileUrl());
        if (!fileStorageUtil.exists(cv.getFileUrl())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Stored CV file not found");
        }
        return path;
    }

    @Transactional
    public void withdrawSubmission(Long jobOfferId, User user) {
        CV cv = findLatestSubmission(jobOfferId, user);
        CandidateEvaluation evaluation = cv.getCandidateEvaluation();
        if (evaluation != null) {
            cv.setCandidateEvaluation(null);
            cvdao.save(cv);
            candidateEvaluationDAO.delete(evaluation);
        }
        if (cv.getFileUrl() != null && !cv.getFileUrl().isBlank()) {
            fileStorageUtil.deleteIfExists(cv.getFileUrl());
        }
        cvdao.delete(cv);
    }

    private Candidate extractCandidate(User user) {
        if (!(user instanceof Candidate candidate)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only candidates can access this resource");
        }
        return candidate;
    }
}



package org.djezzy.pfe.service;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.CVDAO;
import org.djezzy.pfe.dao.CandidateEvaluationDAO;
import org.djezzy.pfe.dto.CandidateSubmissionDTO;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.UploadCvResponseDTO;
import org.djezzy.pfe.event.CvUploadedEvent;
import org.djezzy.pfe.model.CV;
import org.djezzy.pfe.model.CVProcessingStatus;
import org.djezzy.pfe.model.Candidate;
import org.djezzy.pfe.model.CandidateEvaluation;
import org.djezzy.pfe.model.EvaluationStatus;
import org.djezzy.pfe.model.JobOffer;
import org.djezzy.pfe.model.User;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.FileStorageUtil;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
        if (!(user instanceof Candidate candidate)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only candidates can upload CVs");
        }

        JobOffer jobOffer = jobOfferService.findJobOffer(jobOfferId);
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
        return mapperUtil.toCandidateSubmissionDtos(cvdao.findByCandidateIdOrderByUploadDateDesc(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<JobOfferDTO> browseJobOffers() {
        return jobOfferService.listPublicJobOffers();
    }
}

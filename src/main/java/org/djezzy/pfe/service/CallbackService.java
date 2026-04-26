package org.djezzy.pfe.service;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.CVDAO;
import org.djezzy.pfe.dao.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.MatchScoreDAO;
import org.djezzy.pfe.dto.CandidateEvaluationDTO;
import org.djezzy.pfe.dto.EvaluationCallbackRequest;
import org.djezzy.pfe.dto.JobOfferDTO;
import org.djezzy.pfe.dto.StructuredJdCallbackRequest;
import org.djezzy.pfe.model.CV;
import org.djezzy.pfe.model.CVProcessingStatus;
import org.djezzy.pfe.model.CandidateEvaluation;
import org.djezzy.pfe.model.EvaluationStatus;
import org.djezzy.pfe.model.MatchScore;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.MapperUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallbackService {
    private final JobOfferService jobOfferService;
    private final CandidateEvaluationDAO candidateEvaluationDAO;
    private final MatchScoreDAO matchScoreDAO;
    private final CVDAO cvdao;
    private final MapperUtil mapperUtil;

    @Transactional
    public JobOfferDTO handleStructuredJdCallback(Long jobOfferId, StructuredJdCallbackRequest request) {
        return jobOfferService.applyStructuredJdCallback(jobOfferId, request);
    }

    @Transactional
    public CandidateEvaluationDTO handleEvaluationCallback(Long evaluationId, EvaluationCallbackRequest request) {
        CandidateEvaluation evaluation = candidateEvaluationDAO.findById(evaluationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Candidate evaluation not found"));
        evaluation.setStatus(request.status());
        if (request.detailsJson() != null) {
            evaluation.setDetailsJson(request.detailsJson());
        }

        if (request.overallScore() != null) {
            MatchScore matchScore = evaluation.getMatchScore() == null ? new MatchScore() : evaluation.getMatchScore();
            matchScore.setOverallScore(request.overallScore());
            matchScoreDAO.save(matchScore);
            evaluation.setMatchScore(matchScore);
        }

        candidateEvaluationDAO.save(evaluation);
        CV cv = evaluation.getCv();
        if (cv != null) {
            if (request.status() == EvaluationStatus.SCORED) {
                cv.setStatus(CVProcessingStatus.EVALUATED);
            } else if (request.status() == EvaluationStatus.FAILED) {
                cv.setStatus(CVProcessingStatus.FAILED);
            }
            cvdao.save(cv);
        }
        return mapperUtil.toCandidateEvaluationDto(evaluation);
    }
}

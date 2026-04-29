package org.djezzy.pfe.service.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.auth.UserDAO;
import org.djezzy.pfe.dto.system.DashboardStatsDTO;
import org.djezzy.pfe.dto.evaluation.HrEvaluationSummaryDTO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.evaluation.MatchScore;
import org.djezzy.pfe.model.auth.RhApprovalStatus;
import org.djezzy.pfe.model.auth.Role;
import org.djezzy.pfe.util.AppException;
import org.djezzy.pfe.util.FileStorageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

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
    public Page<HrEvaluationSummaryDTO> listEvaluations(Pageable pageable, Long jobId, Double minScore) {
        Specification<CandidateEvaluation> specification = Specification.where(null);
        if (jobId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.join("cv").join("jobOffer").get("id"), jobId)
            );
        }
        if (minScore != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.join("matchScore", jakarta.persistence.criteria.JoinType.LEFT).get("overallScore"), minScore)
            );
        }
        return candidateEvaluationDAO.findAll(specification, pageable)
                .map(this::toSummaryDto);
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

    private HrEvaluationSummaryDTO toSummaryDto(CandidateEvaluation evaluation) {
        CV cv = evaluation.getCv();
        MatchScore score = evaluation.getMatchScore();
        return new HrEvaluationSummaryDTO(
                evaluation.getId(),
                evaluation.getStatus(),
                score == null ? null : score.getOverallScore(),
                cv == null ? null : cv.getId(),
                cv == null ? null : cv.getUploadDate(),
                cv == null || cv.getCandidate() == null ? null : cv.getCandidate().getId(),
                cv == null || cv.getCandidate() == null ? null : cv.getCandidate().getFirstName() + " " + cv.getCandidate().getLastName(),
                cv == null || cv.getJobOffer() == null ? null : cv.getJobOffer().getId(),
                cv == null || cv.getJobOffer() == null ? null : cv.getJobOffer().getTitle()
        );
    }
}



package org.djezzy.pfe.dto.evaluation;

import org.djezzy.pfe.dto.job.*;

import org.djezzy.pfe.model.evaluation.CVProcessingStatus;

import java.time.Instant;

public record CandidateSubmissionDTO(
        Long cvId,
        String fileUrl,
        String rawText,
        CVProcessingStatus cvStatus,
        Instant uploadDate,
        JobOfferDTO jobOffer,
        CandidateEvaluationDTO evaluation
) {
}





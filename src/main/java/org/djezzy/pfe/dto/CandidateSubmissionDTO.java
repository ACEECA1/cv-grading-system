package org.djezzy.pfe.dto;

import org.djezzy.pfe.model.CVProcessingStatus;

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

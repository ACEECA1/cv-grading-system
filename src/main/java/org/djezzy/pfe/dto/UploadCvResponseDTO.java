package org.djezzy.pfe.dto;

import org.djezzy.pfe.model.CVProcessingStatus;
import org.djezzy.pfe.model.EvaluationStatus;

import java.time.Instant;

public record UploadCvResponseDTO(
        Long cvId,
        Long evaluationId,
        CVProcessingStatus cvStatus,
        EvaluationStatus evaluationStatus,
        Instant uploadDate
) {
}

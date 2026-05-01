package org.djezzy.pfe.dto.evaluation;


import org.djezzy.pfe.model.evaluation.CVProcessingStatus;
import org.djezzy.pfe.model.evaluation.EvaluationStatus;

import java.time.Instant;

public record UploadCvResponseDTO(
        Long cvId,
        Long evaluationId,
        CVProcessingStatus cvStatus,
        EvaluationStatus evaluationStatus,
        Instant uploadDate
) {
}





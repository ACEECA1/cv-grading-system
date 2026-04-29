package org.djezzy.pfe.dto.job;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import org.djezzy.pfe.model.job.JobOfferStatus;

import java.time.Instant;

public record JobOfferDTO(
        Long id,
        String title,
        String rawText,
        JobOfferStatus status,
        String jdRequestId,
        StructuredJdDTO structuredJd,
        Instant createdAt,
        Instant updatedAt
) {
}





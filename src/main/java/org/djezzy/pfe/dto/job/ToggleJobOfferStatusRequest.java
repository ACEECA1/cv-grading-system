package org.djezzy.pfe.dto.job;

import jakarta.validation.constraints.NotNull;
import org.djezzy.pfe.model.job.JobOfferStatus;

public record ToggleJobOfferStatusRequest(
        @NotNull JobOfferStatus status
) {
}

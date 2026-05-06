package org.djezzy.pfe.dto.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateJobOfferRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Valid StructuredJdUpdateRequest structuredJd
) {
    public record StructuredJdUpdateRequest(
            String workLocation,
            String employmentType,
            @Valid ExperienceRangeDTO experienceRange,
            List<String> requiredSkills,
            List<String> preferredSkills,
            List<String> responsibilities
    ) {
    }
}

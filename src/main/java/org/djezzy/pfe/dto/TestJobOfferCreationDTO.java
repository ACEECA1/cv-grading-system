package org.djezzy.pfe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestJobOfferCreationDTO(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(min = 20) String rawText,
        @NotBlank @Size(max = 255) String companyName,
        @NotNull ExperienceRangeDTO experienceRange,
        @NotBlank @Size(max = 255) String workLocation,
        @NotEmpty List<@NotBlank String> requiredSkills,
        List<@NotBlank String> preferredSkills
) {
}
